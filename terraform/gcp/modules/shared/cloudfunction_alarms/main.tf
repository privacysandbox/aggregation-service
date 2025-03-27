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

resource "google_monitoring_alert_policy" "error_count_5xx" {
  display_name = "${var.service_prefix} Cloud Function 5xx Errors"
  combiner     = "OR"
  conditions {
    display_name = "5xx Errors"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_count\" AND resource.type=\"cloud_run_revision\" AND resource.label.service_name=\"${var.function_name}\" AND metric.label.response_code_class=\"5xx\""
      duration        = "${var.duration_sec}s"
      comparison      = "COMPARISON_GT"
      threshold_value = var.error_5xx_threshold
      trigger {
        count = 1
      }
      aggregations {
        alignment_period   = "${var.eval_period_sec}s"
        per_series_aligner = "ALIGN_SUM"
      }
    }
  }
  notification_channels = [var.notification_channel_id]

  user_labels = {
    environment = var.environment
  }
}

resource "google_monitoring_alert_policy" "execution_times" {
  display_name = "${var.service_prefix} Cloud Function Execution Times"
  combiner     = "OR"
  conditions {
    display_name = "Execution Times"
    condition_threshold {
      filter     = "metric.type=\"cloudfunctions.googleapis.com/function/execution_times\" AND resource.type=\"cloud_function\" AND resource.label.function_name=\"${var.function_name}\""
      duration   = "${var.duration_sec}s"
      comparison = "COMPARISON_GT"
      # Cloud function alerts use nanoseconds
      threshold_value = var.execution_time_max * 1000000
      trigger {
        count = 1
      }
      aggregations {
        alignment_period   = "${var.eval_period_sec}s"
        per_series_aligner = "ALIGN_PERCENTILE_99"
      }
    }
  }
  notification_channels = [var.notification_channel_id]

  user_labels = {
    environment = var.environment
  }
}

resource "google_monitoring_alert_policy" "error_count" {
  display_name = "${var.service_prefix} Cloud Function Execution Errors"
  combiner     = "OR"
  conditions {
    display_name = "Execution Errors"
    condition_threshold {
      filter          = "metric.type=\"cloudfunctions.googleapis.com/function/execution_count\" AND resource.type=\"cloud_function\" AND resource.label.function_name=\"${var.function_name}\" AND metric.label.status!=\"ok\""
      duration        = "${var.duration_sec}s"
      comparison      = "COMPARISON_GT"
      threshold_value = var.execution_error_threshold
      trigger {
        count = 1
      }
      aggregations {
        alignment_period   = "${var.eval_period_sec}s"
        per_series_aligner = "ALIGN_SUM"
      }
    }
  }
  notification_channels = [var.notification_channel_id]

  user_labels = {
    environment = var.environment
  }
}
