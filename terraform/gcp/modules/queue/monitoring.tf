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
  subscription_id = google_pubsub_subscription.queue_sub.name
}

resource "google_monitoring_alert_policy" "queue_undelivered_message_too_old_alert" {
  count        = var.alarms_enabled ? 1 : 0
  display_name = "${var.environment} Queue Message Too Old"
  combiner     = "OR"
  conditions {
    display_name = "Oldest Undelivered Message Age"
    condition_threshold {
      filter          = "resource.type = \"pubsub_subscription\" AND resource.labels.subscription_id = \"${local.subscription_id}\" AND metric.type = \"pubsub.googleapis.com/subscription/oldest_unacked_message_age\""
      duration        = "0s"
      comparison      = "COMPARISON_GT"
      threshold_value = var.max_undelivered_message_age_sec
      trigger {
        count = 1
      }
      aggregations {
        alignment_period     = "${var.alarm_eval_period_sec}s"
        per_series_aligner   = "ALIGN_MAX"
        cross_series_reducer = "REDUCE_MAX"
        group_by_fields      = ["resource.label.\"subscription_id\""]
      }
    }
  }
  notification_channels = [var.notification_channel_id]

  user_labels = {
    environment = var.environment
  }
}
