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
  cloudfunction_name_suffix = "frontend_service_cloudfunction"
  cloudfunction_package_zip = "${var.frontend_service_jar}.zip"
}

resource "google_service_account" "frontend_service_account" {
  # Service account id has a 30 character limit
  account_id   = "${var.environment}-frontend"
  display_name = "Frontend Service Account"
}

# Archives the JAR in a ZIP file
data "archive_file" "frontend_service_archive" {
  type        = "zip"
  source_file = var.frontend_service_jar
  output_path = local.cloudfunction_package_zip
}

resource "google_storage_bucket_object" "frontend_service_package_bucket_object" {
  # Need hash in name so cloudfunction knows to redeploy when code changes
  name   = "${var.environment}_${local.cloudfunction_name_suffix}_${data.archive_file.frontend_service_archive.output_md5}"
  bucket = var.operator_package_bucket_name
  source = local.cloudfunction_package_zip
}

resource "google_cloudfunctions2_function" "frontend_service_cloudfunction" {
  name     = "${var.environment}-${var.region}-frontend-service"
  location = var.region

  build_config {
    runtime     = "java17"
    entry_point = "com.google.scp.operator.frontend.service.gcp.FrontendServiceHttpFunction"
    source {
      storage_source {
        bucket = var.operator_package_bucket_name
        object = google_storage_bucket_object.frontend_service_package_bucket_object.name
      }
    }
  }

  service_config {
    min_instance_count            = var.frontend_service_cloudfunction_min_instances
    max_instance_count            = var.frontend_service_cloudfunction_max_instances
    timeout_seconds               = var.frontend_service_cloudfunction_timeout_sec
    available_memory              = "${var.frontend_service_cloudfunction_memory_mb}M"
    service_account_email         = google_service_account.frontend_service_account.email
    vpc_connector                 = var.vpc_connector_id
    vpc_connector_egress_settings = var.vpc_connector_id == null ? null : "ALL_TRAFFIC"
    environment_variables = {
      PROJECT_ID             = var.project_id
      INSTANCE_ID            = var.spanner_instance_name
      DATABASE_ID            = var.spanner_database_name
      PUBSUB_TOPIC_ID        = var.job_queue_topic
      PUBSUB_SUBSCRIPTION_ID = var.job_queue_sub
      JOB_METADATA_TTL       = var.job_metadata_table_ttl_days
      JOB_VERSION            = var.job_version
      LOG_EXECUTION_ID       = true
    }
  }

  labels = {
    environment = var.environment
  }
}

# JobMetadata read/write permissions
resource "google_spanner_database_iam_member" "frontend_service_jobmetadatadb_iam" {
  instance = var.spanner_instance_name
  database = var.spanner_database_name
  role     = "roles/spanner.databaseUser"
  member   = "serviceAccount:${google_service_account.frontend_service_account.email}"
}

# JobMetadata read/write permissions
resource "google_pubsub_topic_iam_member" "frontend_service_jobqueue_iam" {
  role   = "roles/pubsub.publisher"
  member = "serviceAccount:${google_service_account.frontend_service_account.email}"
  topic  = var.job_queue_topic
}
