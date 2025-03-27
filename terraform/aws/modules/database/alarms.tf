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

resource "aws_cloudwatch_metric_alarm" "table_read_capacity_usage_ratio_alarm" {
  count             = var.alarms_enabled ? 1 : 0
  alarm_name        = "${var.environment}_${var.read_alarm_name}"
  alarm_description = "Table has consumed more than its read capacity units."

  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = var.read_capacity_usage_ratio_alarm_threshold

  evaluation_periods = 1

  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"
  alarm_actions             = [var.sns_topic_arn]

  metric_query {
    id          = "e1"
    expression  = "m1/(${var.read_capacity}*${var.eval_period_sec})"
    label       = "${var.environment} Read Capacity Usage Ratio"
    return_data = "true"
  }

  metric_query {
    id = "m1"

    metric {
      metric_name = "ConsumedReadCapacityUnits"
      namespace   = "AWS/DynamoDB"
      period      = var.eval_period_sec
      stat        = "Sum"

      dimensions = {
        TableName = var.table_name
      }
    }
  }

  tags = {
    environment = var.environment
  }
}

resource "aws_cloudwatch_metric_alarm" "table_write_capacity_usage_ratio_alarm" {
  count             = var.alarms_enabled ? 1 : 0
  alarm_name        = "${var.environment}_${var.write_alarm_name}"
  alarm_description = "Table has consumed more than its write capacity units."

  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = var.write_capacity_usage_ratio_alarm_threshold

  evaluation_periods = 1

  insufficient_data_actions = []
  treat_missing_data        = "notBreaching"
  alarm_actions             = [var.sns_topic_arn]

  metric_query {
    id          = "e1"
    expression  = "m1/(${var.write_capacity}*${var.eval_period_sec})"
    label       = "${var.environment} Write Capacity Usage Ratio"
    return_data = "true"
  }

  metric_query {
    id = "m1"

    metric {
      metric_name = "ConsumedWriteCapacityUnits"
      namespace   = "AWS/DynamoDB"
      period      = var.eval_period_sec
      stat        = "Sum"

      dimensions = {
        TableName = var.table_name
      }
    }
  }

  tags = {
    environment = var.environment
  }
}
