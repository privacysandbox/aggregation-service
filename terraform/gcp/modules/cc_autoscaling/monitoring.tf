/**
 * Copyright 2023 Google LLC
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

resource "google_monitoring_alert_policy" "autoscaling_max_instances_alarm" {
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
