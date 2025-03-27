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
  function_name = google_cloudfunctions2_function.frontend_service_cloudfunction.name
}

module "frontendservice_cloudfunction_alarms" {
  source = "../shared/cloudfunction_alarms"
  count  = var.alarms_enabled ? 1 : 0

  environment             = var.environment
  notification_channel_id = var.notification_channel_id
  function_name           = local.function_name
  service_prefix          = "${var.environment} Frontend Service"

  eval_period_sec = var.alarm_eval_period_sec
  // This will alert if 5xx errors amount in frontend service cloudfunction is greater than threshold.
  error_5xx_threshold = var.cloudfunction_5xx_threshold
  // This will alert if execution time in frontend service cloudfunction is greater than threshold.
  execution_time_max = var.cloudfunction_max_execution_time_max
  // This will alert if errors amount in frontend service cloudfunction is greater than threshold.
  execution_error_threshold = var.cloudfunction_error_threshold
  duration_sec              = var.alarm_duration_sec
}

resource "google_monitoring_dashboard" "frontend_dashboard" {
  dashboard_json = jsonencode(
    {
      "displayName" : "${var.environment} Frontend Dashboard",
      "gridLayout" : {
        "columns" : "2",
        "widgets" : flatten([
          {
            "title" : "Cloud Function Executions",
            "xyChart" : {
              "chartOptions" : {
                "mode" : "COLOR"
              },
              "dataSets" : [
                {
                  "legendTemplate" : "Executions",
                  "minAlignmentPeriod" : "60s",
                  "plotType" : "LINE",
                  "targetAxis" : "Y2",
                  "timeSeriesQuery" : {
                    "timeSeriesFilter" : {
                      "aggregation" : {
                        "alignmentPeriod" : "60s",
                        "perSeriesAligner" : "ALIGN_RATE"
                      },
                      "filter" : "metric.type=\"run.googleapis.com/request_count\" resource.type=\"cloud_run_revision\" resource.label.\"service_name\"=\"${local.function_name}\"",
                      "secondaryAggregation" : {
                        "alignmentPeriod" : "60s",
                        "crossSeriesReducer" : "REDUCE_SUM",
                        "groupByFields" : [
                          "resource.label.\"service_name\""
                        ],
                        "perSeriesAligner" : "ALIGN_MEAN"
                      }
                    }
                  }
                }
              ],
              "timeshiftDuration" : "0s",
              "y2Axis" : {
                "label" : "y2Axis",
                "scale" : "LINEAR"
              }
            }
          },
          {
            "title" : "Cloud Function Execution Times",
            "xyChart" : {
              "chartOptions" : {
                "mode" : "COLOR"
              },
              "dataSets" : [
                {
                  "legendTemplate" : "p99",
                  "minAlignmentPeriod" : "60s",
                  "plotType" : "LINE",
                  "targetAxis" : "Y2",
                  "timeSeriesQuery" : {
                    "timeSeriesFilter" : {
                      "aggregation" : {
                        "alignmentPeriod" : "60s",
                        "perSeriesAligner" : "ALIGN_PERCENTILE_99"
                      },
                      "filter" : "metric.type=\"cloudfunctions.googleapis.com/function/execution_times\" resource.type=\"cloud_function\" resource.label.\"function_name\"=\"${local.function_name}\"",
                      "secondaryAggregation" : {
                        "alignmentPeriod" : "60s",
                        "crossSeriesReducer" : "REDUCE_MEAN",
                        "groupByFields" : [
                          "resource.label.\"function_name\""
                        ],
                        "perSeriesAligner" : "ALIGN_MEAN"
                      }
                    }
                  }
                },
                {
                  "legendTemplate" : "p95",
                  "minAlignmentPeriod" : "60s",
                  "plotType" : "LINE",
                  "targetAxis" : "Y2",
                  "timeSeriesQuery" : {
                    "timeSeriesFilter" : {
                      "aggregation" : {
                        "alignmentPeriod" : "60s",
                        "perSeriesAligner" : "ALIGN_PERCENTILE_95"
                      },
                      "filter" : "metric.type=\"cloudfunctions.googleapis.com/function/execution_times\" resource.type=\"cloud_function\" resource.label.\"function_name\"=\"${local.function_name}\"",
                      "secondaryAggregation" : {
                        "alignmentPeriod" : "60s",
                        "crossSeriesReducer" : "REDUCE_MEAN",
                        "groupByFields" : [
                          "resource.label.\"function_name\""
                        ],
                        "perSeriesAligner" : "ALIGN_MEAN"
                      }
                    }
                  }
                },
                {
                  "legendTemplate" : "p50",
                  "minAlignmentPeriod" : "60s",
                  "plotType" : "LINE",
                  "targetAxis" : "Y2",
                  "timeSeriesQuery" : {
                    "timeSeriesFilter" : {
                      "aggregation" : {
                        "alignmentPeriod" : "60s",
                        "perSeriesAligner" : "ALIGN_PERCENTILE_50"
                      },
                      "filter" : "metric.type=\"cloudfunctions.googleapis.com/function/execution_times\" resource.type=\"cloud_function\" resource.label.\"function_name\"=\"${local.function_name}\"",
                      "secondaryAggregation" : {
                        "alignmentPeriod" : "60s",
                        "crossSeriesReducer" : "REDUCE_MEAN",
                        "groupByFields" : [
                          "resource.label.\"function_name\""
                        ],
                        "perSeriesAligner" : "ALIGN_MEAN"
                      }
                    }
                  }
                }
              ],
              "timeshiftDuration" : "0s",
              "y2Axis" : {
                "label" : "y2Axis",
                "scale" : "LINEAR"
              }
            }
          },
          {
            "title" : "Cloud Function Errors",
            "xyChart" : {
              "chartOptions" : {
                "mode" : "COLOR"
              },
              "dataSets" : [
                {
                  "legendTemplate" : "\u0024{metric.labels.response_code_class}",
                  "minAlignmentPeriod" : "60s",
                  "plotType" : "LINE",
                  "targetAxis" : "Y2",
                  "timeSeriesQuery" : {
                    "timeSeriesFilter" : {
                      "aggregation" : {
                        "alignmentPeriod" : "60s",
                        "perSeriesAligner" : "ALIGN_RATE"
                      },
                      "filter" : "metric.type=\"run.googleapis.com/request_count\" resource.type=\"cloud_run_revision\" resource.label.\"service_name\"=\"${local.function_name}\" metric.label.\"response_code_class\"!=\"2xx\"",
                      "secondaryAggregation" : {
                        "alignmentPeriod" : "60s",
                        "crossSeriesReducer" : "REDUCE_SUM",
                        "groupByFields" : [
                          "metric.label.\"response_code_class\""
                        ],
                        "perSeriesAligner" : "ALIGN_MEAN"
                      }
                    }
                  }
                }
              ],
              "timeshiftDuration" : "0s",
              "y2Axis" : {
                "label" : "y2Axis",
                "scale" : "LINEAR"
              }
            }
          },
          {
            "title" : "Cloud Function Max Concurrent Requests",
            "xyChart" : {
              "chartOptions" : {
                "mode" : "COLOR"
              },
              "dataSets" : [
                {
                  "legendTemplate" : "Max Concurrent Requests",
                  "minAlignmentPeriod" : "60s",
                  "plotType" : "LINE",
                  "targetAxis" : "Y2",
                  "timeSeriesQuery" : {
                    "timeSeriesFilter" : {
                      "aggregation" : {
                        "alignmentPeriod" : "60s",
                        "crossSeriesReducer" : "REDUCE_MEAN",
                        "groupByFields" : [
                          "resource.label.\"service_name\""
                        ],
                        "perSeriesAligner" : "ALIGN_SUM"
                      },
                      "filter" : "metric.type=\"run.googleapis.com/container/max_request_concurrencies\" resource.type=\"cloud_run_revision\" resource.label.\"service_name\"=\"${local.function_name}\" metric.label.\"state\"=\"active\"",
                      "secondaryAggregation" : {
                        "alignmentPeriod" : "60s",
                      }
                    }
                  }
                }
              ],
              "timeshiftDuration" : "0s",
              "y2Axis" : {
                "label" : "y2Axis",
                "scale" : "LINEAR"
              }
            }
          },
        ])
      }
    }
  )
}
