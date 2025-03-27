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

################################################################################
# Lambda Alarms
################################################################################

#Error alarm. Keeps track of asg capacity lambda instances resulting in an error
resource "aws_cloudwatch_metric_alarm" "asg_capacity_lambda_error_alarm" {
  count               = var.enable_autoscaling && var.worker_alarms_enabled ? 1 : 0
  alarm_name          = "${var.environment}_${var.region}_asg_capacity_lambda_error_alarm"
  alarm_description   = "This alarm will be triggered if the error count of the autoscaling group capacity lambda is greater than the threshold of ${var.asg_capacity_lambda_error_threshold}."
  comparison_operator = "GreaterThanThreshold"
  #Number of 'period' to evaluate for the alarm
  evaluation_periods        = 1
  threshold                 = var.asg_capacity_lambda_error_threshold
  insufficient_data_actions = []
  metric_name               = "Errors"
  namespace                 = "AWS/Lambda"
  period                    = var.asg_capacity_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.asg_capacity_lambda.function_name
  }
  alarm_actions = [var.operator_sns_topic_arn]
}

#Duration alarm. For timeouts for asg capacity lambda instances.
resource "aws_cloudwatch_metric_alarm" "asg_capacity_lambda_max_duration_alarm" {
  count                     = var.enable_autoscaling && var.worker_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_asg_capacity_lambda_duration_alarm"
  alarm_description         = "The alarm will be triggered if event processing time spent by the autoscaling group lambda is more than the duration threshold of ${var.asg_capacity_lambda_duration_threshold}ms"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.asg_capacity_lambda_duration_threshold
  insufficient_data_actions = []
  metric_name               = "Duration"
  namespace                 = "AWS/Lambda"
  period                    = var.asg_capacity_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "breaching"

  dimensions = {
    FunctionName = aws_lambda_function.asg_capacity_lambda.function_name
  }
  alarm_actions = [var.operator_sns_topic_arn]
}

#Error alarm. Keeps track of lambda instances resulting in an error
resource "aws_cloudwatch_metric_alarm" "terminated_instance_lambda_error_alarm" {
  count               = var.enable_autoscaling && var.worker_alarms_enabled ? 1 : 0
  alarm_name          = "${var.environment}_${var.region}_terminated_instance_lambda_error_alarm"
  alarm_description   = "This alarm will be triggered if the error of the terminated instance lambda is greater than the threshold of ${var.terminated_instance_lambda_error_threshold}."
  comparison_operator = "GreaterThanThreshold"
  #Number of 'period' to evaluate for the alarm
  evaluation_periods        = 1
  threshold                 = var.terminated_instance_lambda_error_threshold
  insufficient_data_actions = []
  metric_name               = "Errors"
  namespace                 = "AWS/Lambda"
  period                    = var.terminated_instance_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.terminated_instance_lambda.function_name
  }
  alarm_actions = [var.operator_sns_topic_arn]
}

#Duration alarm. For timeouts
resource "aws_cloudwatch_metric_alarm" "terminated_instance_lambda_max_duration_alarm" {
  count                     = var.enable_autoscaling && var.worker_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_terminated_instance_lambda_duration_alarm"
  alarm_description         = "This alarm will be triggered if the amount of time the terminated instance lambda spend is greater than threshold."
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.terminated_instance_lambda_duration_threshold
  insufficient_data_actions = []
  metric_name               = "Duration"
  namespace                 = "AWS/Lambda"
  period                    = var.terminated_instance_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.terminated_instance_lambda.function_name
  }
  alarm_actions = [var.operator_sns_topic_arn]
}

################################################################################
# Auto Scaling Group Alarms
################################################################################

resource "aws_cloudwatch_metric_alarm" "asg_max_instances_alarm" {
  count               = var.enable_autoscaling && var.worker_alarms_enabled ? 1 : 0
  alarm_name          = "${var.environment}_${var.region}_asg_max_instance_alarm"
  alarm_description   = "This alarm will be triggered if the number of instances that the Auto Scaling group attempts to maintain is greater than the threshold."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  threshold           = ceil(var.max_ec2_instances * var.asg_max_instances_alarm_ratio)
  metric_name         = "GroupDesiredCapacity"
  namespace           = "AWS/AutoScaling"
  period              = var.autoscaling_alarm_eval_period_sec
  statistic           = "Maximum"

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.worker_group.name
  }
  alarm_actions = [var.operator_sns_topic_arn]
}
