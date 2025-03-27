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
  asg_instance_group_name = google_compute_region_instance_group_manager.worker_instance_group.name
}

module "autoscaling_cloudfunction_alarms" {
  source = "../shared/cloudfunction_alarms"
  count  = var.alarms_enabled ? 1 : 0

  environment             = var.environment
  notification_channel_id = var.notification_channel_id
  function_name           = google_cloudfunctions2_function.worker_scale_in_cloudfunction.name
  service_prefix          = "${var.environment} Autoscaling Cloud Function"

  eval_period_sec = var.alarm_eval_period_sec
  // This will alert if 5xx errors amount in autoscaling cloudfunction is greater than threshold.
  error_5xx_threshold = var.cloudfunction_5xx_threshold
  // This will alert if execution time in autoscaling cloudfunction is greater than threshold.
  execution_time_max = var.cloudfunction_max_execution_time_ms
  // This will alert if errors amount in autoscaling cloudfunction is greater than threshold.
  execution_error_threshold = var.cloudfunction_error_threshold
  duration_sec              = var.alarm_duration_sec
}

resource "google_monitoring_alert_policy" "autoscaling_max_instances_alarm" {
  // The alarm will be triggered if the number of instances is more than threshold.
  count        = var.alarms_enabled ? 1 : 0
  display_name = "${var.environment} Autoscaling Instance Count Too High"
  combiner     = "OR"
  conditions {
    display_name = "Autoscaling Instance Count"
    condition_threshold {
      filter          = "resource.type = \"instance_group\" AND resource.labels.instance_group_name = \"${local.asg_instance_group_name}\" AND metric.type = \"compute.googleapis.com/instance_group/size\""
      duration        = "${var.alarm_duration_sec}s"
      comparison      = "COMPARISON_GT"
      threshold_value = ceil(var.max_worker_instances * var.max_vm_instances_ratio_threshold)
      trigger {
        count = 1
      }
      aggregations {
        alignment_period     = "${var.alarm_eval_period_sec}s"
        per_series_aligner   = "ALIGN_MAX"
        cross_series_reducer = "REDUCE_SUM"
        group_by_fields      = ["resource.label.\"instance_group_name\""]
      }
    }
  }
  notification_channels = [var.notification_channel_id]

  user_labels = {
    environment = var.environment
  }
}
