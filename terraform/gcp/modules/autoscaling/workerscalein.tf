/**
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
locals {
  cloudfunction_name_suffix = "worker-scale-in-cloudfunction"
  cloudfunction_package_zip = "${var.worker_scale_in_jar}.zip"
}

# Archives the JAR in a ZIP file
data "archive_file" "worker_scale_in_archive" {
  type        = "zip"
  source_file = var.worker_scale_in_jar
  output_path = local.cloudfunction_package_zip
}

resource "google_storage_bucket_object" "worker_scale_in_package_bucket_object" {
  # Need hash in name so cloudfunction knows to redeploy when code changes
  name   = "${var.environment}_${local.cloudfunction_name_suffix}_${data.archive_file.worker_scale_in_archive.output_md5}"
  bucket = var.operator_package_bucket_name
  source = local.cloudfunction_package_zip
}

resource "google_cloudfunctions2_function" "worker_scale_in_cloudfunction" {
  name     = "${var.environment}-${var.region}-worker-scale-in"
  location = var.region

  build_config {
    runtime     = "java17"
    entry_point = "com.google.scp.operator.autoscaling.app.gcp.WorkerScaleInHttpFunction"
    source {
      storage_source {
        bucket = var.operator_package_bucket_name
        object = google_storage_bucket_object.worker_scale_in_package_bucket_object.name
      }
    }
  }

  service_config {
    # Only one instance should run to control instance group scale-in
    max_instance_count            = 1
    timeout_seconds               = 180
    available_memory              = "${var.autoscaling_cloudfunction_memory_mb}M"
    service_account_email         = var.worker_service_account
    ingress_settings              = "ALLOW_ALL" # Otherwise, it cannot be triggered by Cloud Scheduler.
    vpc_connector                 = var.vpc_connector_id
    vpc_connector_egress_settings = var.vpc_connector_id == null ? null : "ALL_TRAFFIC"
    environment_variables = {
      PROJECT_ID                  = var.project_id
      REGION                      = var.region
      SPANNER_INSTANCE_ID         = var.metadatadb_instance_name
      SPANNER_DATABASE_ID         = var.metadatadb_name
      MANAGED_INSTANCE_GROUP_NAME = google_compute_region_instance_group_manager.worker_instance_group.name
      TERMINATION_WAIT_TIMEOUT    = var.termination_wait_timeout_sec
      ASG_INSTANCES_TTL           = var.asg_instances_table_ttl_days
      LOG_EXECUTION_ID            = true
    }
  }

  labels = {
    environment = var.environment
  }
}

resource "google_cloud_scheduler_job" "worker_scale_in_scheduler" {
  name        = "${var.environment}-scale-in-sched"
  description = "The worker scale-in scheduler."
  schedule    = var.worker_scale_in_cron

  http_target {
    http_method = "POST"
    uri         = "${google_cloudfunctions2_function.worker_scale_in_cloudfunction.service_config[0].uri}/manageInstances"
    oidc_token {
      service_account_email = var.worker_service_account
    }
  }
}

resource "google_cloud_run_service_iam_member" "worker_scale_in_sched_iam" {
  location = var.region
  project  = var.project_id
  service  = google_cloudfunctions2_function.worker_scale_in_cloudfunction.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${var.worker_service_account}"
}
