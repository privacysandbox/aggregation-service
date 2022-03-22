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
  worker_composite_alarm_rule = (var.worker_alarms_enabled ? <<EOF
ALARM(${aws_cloudwatch_composite_alarm.job_client_error_composite_alarm[0].alarm_name}) OR
ALARM(${aws_cloudwatch_composite_alarm.job_validation_failure_composite_alarm[0].alarm_name}) OR
ALARM(${aws_cloudwatch_composite_alarm.worker_job_error_composite_alarm[0].alarm_name})
  EOF
  : "")
}

resource "aws_cloudwatch_metric_alarm" "job_client_error_alarm" {
  for_each                  = var.worker_alarms_enabled ? toset(var.job_client_error_reasons) : []
  alarm_name                = "${var.environment}_${var.region}_${each.key}_job_client_alarm"
  alarm_description         = "Job client exception alarm for ${each.key}"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.job_client_error_threshold
  insufficient_data_actions = []
  metric_name               = "JobClientError"
  namespace                 = "${var.environment}/scp/jobclient"
  period                    = var.worker_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    ErrorReason = each.key
  }
}

resource "aws_cloudwatch_composite_alarm" "job_client_error_composite_alarm" {
  count             = var.worker_alarms_enabled ? 1 : 0
  alarm_name        = "${var.environment}_${var.region}_job_client_composite_alarm"
  alarm_description = "Job client exception composite alarm"

  alarm_rule = join(" OR ", formatlist("ALARM(%s)", [for alarm in aws_cloudwatch_metric_alarm.job_client_error_alarm : alarm.alarm_name]))

  depends_on = [aws_cloudwatch_metric_alarm.job_client_error_alarm]
}

resource "aws_cloudwatch_metric_alarm" "job_validation_failure_alarm" {
  for_each                  = var.worker_alarms_enabled ? toset(var.job_validation_types) : []
  alarm_name                = "${var.environment}_${var.region}_${each.key}_job_validation_failure_alarm"
  alarm_description         = "Job validation failure alarm for ${each.key}"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.job_validation_failure_threshold
  insufficient_data_actions = []
  metric_name               = "JobValidationFailure"
  namespace                 = "${var.environment}/scp/jobclient"
  period                    = var.worker_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    Validator = each.key
  }
}

resource "aws_cloudwatch_composite_alarm" "job_validation_failure_composite_alarm" {
  count             = var.worker_alarms_enabled ? 1 : 0
  alarm_name        = "${var.environment}_${var.region}_job_validation_failure_composite_alarm"
  alarm_description = "Job validation failure composite alarm"

  alarm_rule = join(" OR ", formatlist("ALARM(%s)",
  [for alarm in aws_cloudwatch_metric_alarm.job_validation_failure_alarm : alarm.alarm_name]))

  depends_on = [aws_cloudwatch_metric_alarm.job_validation_failure_alarm]
}

resource "aws_cloudwatch_metric_alarm" "worker_job_error_metric" {
  for_each                  = var.worker_alarms_enabled ? toset(var.worker_alarm_metric_dimensions) : []
  alarm_name                = "${var.environment}_${var.region}_${each.key}_worker_job_alarm"
  alarm_description         = "General job processing exception alarm"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.worker_job_error_threshold
  insufficient_data_actions = []
  metric_name               = "WorkerJobError"
  namespace                 = "${var.environment}/scp/worker"
  period                    = var.worker_alarm_eval_period_sec
  statistic                 = "Sum"

  dimensions = {
    Type = each.key
  }

  treat_missing_data = "notBreaching"
}

resource "aws_cloudwatch_composite_alarm" "worker_job_error_composite_alarm" {
  count             = var.worker_alarms_enabled ? 1 : 0
  alarm_name        = "${var.environment}_${var.region}_worker_job_error_composite_alarm"
  alarm_description = "Worker job error alarm composite alarm"

  alarm_rule = join(" OR ", formatlist("ALARM(%s)",
  [for alarm in aws_cloudwatch_metric_alarm.worker_job_error_metric : alarm.alarm_name]))

  depends_on = [aws_cloudwatch_metric_alarm.worker_job_error_metric]
}

resource "aws_cloudwatch_composite_alarm" "worker_composite_alarm" {
  count             = var.worker_alarms_enabled ? 1 : 0
  alarm_name        = "${var.environment}_${var.region}_worker_service_composite_alarm"
  alarm_description = "Entire worker service composite alarm"
  alarm_actions     = [var.operator_sns_topic_arn]

  alarm_rule = trimspace(replace(local.worker_composite_alarm_rule, "/\n+/", " "))

  depends_on = [
    aws_cloudwatch_composite_alarm.job_client_error_composite_alarm,
    aws_cloudwatch_composite_alarm.job_validation_failure_composite_alarm,
    aws_cloudwatch_composite_alarm.worker_job_error_composite_alarm
  ]
}
