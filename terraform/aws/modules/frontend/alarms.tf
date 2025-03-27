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

#Error alarms
resource "aws_cloudwatch_metric_alarm" "get_job_lambda_error_alarm" {
  count               = var.frontend_alarms_enabled ? 1 : 0
  alarm_name          = "${var.environment}_${var.region}_get_job_lambda_error_alarm"
  alarm_description   = "This alarm will be triggered if getJob lambda errors over ${var.frontend_lambda_error_threshold}%"
  comparison_operator = "GreaterThanThreshold"
  #Number of 'period' to evaluate for the alarm
  evaluation_periods        = 1
  threshold                 = var.frontend_lambda_error_threshold
  insufficient_data_actions = []
  metric_name               = "Errors"
  namespace                 = "AWS/Lambda"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.get_job_lambda.function_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

resource "aws_cloudwatch_metric_alarm" "create_job_lambda_error_alarm" {
  count               = var.frontend_alarms_enabled ? 1 : 0
  alarm_name          = "${var.environment}_${var.region}_create_job_lambda_error_alarm"
  alarm_description   = "This alarm will be triggered if createJob lambda errors over ${var.frontend_lambda_error_threshold}%"
  comparison_operator = "GreaterThanThreshold"
  #Number of 'period' to evaluate for the alarm
  evaluation_periods        = 1
  threshold                 = var.frontend_lambda_error_threshold
  insufficient_data_actions = []
  metric_name               = "Errors"
  namespace                 = "AWS/Lambda"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.create_job_lambda.function_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

#Duration alarms
resource "aws_cloudwatch_metric_alarm" "get_job_key_lambda_max_duration_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_get_job_key_lambda_max_duration_alarm"
  alarm_description         = "This alarm will be triggered if getJob lambda duration over ${var.frontend_lambda_max_duration_threshold}ms"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.frontend_lambda_max_duration_threshold
  insufficient_data_actions = []
  metric_name               = "Duration"
  namespace                 = "AWS/Lambda"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.get_job_lambda.function_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

resource "aws_cloudwatch_metric_alarm" "create_job_key_lambda_max_duration_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_create_job_key_lambda_max_duration_alarm"
  alarm_description         = "This alarm will be triggered if createJob lambda duration over ${var.frontend_lambda_max_duration_threshold}ms"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.frontend_lambda_max_duration_threshold
  insufficient_data_actions = []
  metric_name               = "Duration"
  namespace                 = "AWS/Lambda"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.create_job_lambda.function_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

#Throttle alarms
resource "aws_cloudwatch_metric_alarm" "get_job_key_lambda_max_throttles_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_get_job_key_lambda_max_throttles_alarm"
  alarm_description         = "Lambda throttles over ${var.frontend_lambda_max_throttles_threshold}"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.frontend_lambda_max_throttles_threshold
  insufficient_data_actions = []
  metric_name               = "Throttles"
  namespace                 = "AWS/Lambda"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.get_job_lambda.function_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

resource "aws_cloudwatch_metric_alarm" "create_job_key_lambda_max_throttles_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_create_job_key_lambda_max_throttles_alarm"
  alarm_description         = "Lambda throttles over ${var.frontend_lambda_max_throttles_threshold}"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.frontend_lambda_max_throttles_threshold
  insufficient_data_actions = []
  metric_name               = "Throttles"
  namespace                 = "AWS/Lambda"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.create_job_lambda.function_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

#################################################################################
## API Gateway Alarms
#################################################################################
#
#Latency alarm
resource "aws_cloudwatch_metric_alarm" "frontend_api_max_latency_alarm" {
  count               = var.frontend_alarms_enabled ? 1 : 0
  alarm_name          = "${var.environment}_${var.region}_frontend_api_max_latency_alarm"
  alarm_description   = "This alarm will be triggered if frontend API max latency is over ${var.frontend_api_max_latency_ms}ms. Latency measures the time between API gateway receives the request to return a response."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Latency"
  namespace           = "AWS/ApiGateway"
  period              = var.frontend_alarm_eval_period_sec
  #Unit for threshold
  unit      = "Milliseconds"
  threshold = var.frontend_api_max_latency_ms
  statistic = "Maximum"

  dimensions = {
    ApiId = aws_apigatewayv2_api.frontend_api_gateway.id
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

#5xx Errors alarm
resource "aws_cloudwatch_metric_alarm" "frontend_api_5xx_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_frontend_api_5xx_alarm"
  alarm_description         = "This alarm will be triggered if 5xx errors is over ${var.frontend_5xx_threshold}%. Based on AWS official doc, 5XX Error measures 'The number of server-side errors captured in a given period.'"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.frontend_5xx_threshold
  insufficient_data_actions = []
  metric_name               = "5XXError"
  namespace                 = "AWS/ApiGateway"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    ApiId = aws_apigatewayv2_api.frontend_api_gateway.id
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

#4xx Errors alarm
resource "aws_cloudwatch_metric_alarm" "frontend_api_4xx_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_frontend_api_4xx_alarm"
  alarm_description         = "This alarm will be triggered if 4xx errors is over ${var.frontend_4xx_threshold}%. Based on AWS official doc, 4XX Error measures 'The number of client-side errors captured in a given period.'"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.frontend_4xx_threshold
  insufficient_data_actions = []
  metric_name               = "4XXError"
  namespace                 = "AWS/ApiGateway"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    ApiId = aws_apigatewayv2_api.frontend_api_gateway.id
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

#################################################################################
## Dynamo Streams Alarms
#################################################################################

resource "aws_cloudwatch_metric_alarm" "change_handler_latency_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_change_handler_latency_alarm"
  alarm_description         = "This alarm will be triggered if max latency is over ${var.change_handler_max_latency_ms}ms. Based on the AWS official doc, this metric measures 'the time between when a stream receives the record and when the event source mapping sends the event to the function.'"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.change_handler_max_latency_ms
  insufficient_data_actions = []
  metric_name               = "IteratorAge"
  namespace                 = "AWS/Lambda"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.change_handler_lambda.function_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}

#################################################################################
## Change Handler DLQ Alarms
#################################################################################

#Change Handler DLQ Alarms
resource "aws_cloudwatch_metric_alarm" "change_handler_dlq_alarm" {
  count                     = var.frontend_alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_change_handler_dlq_alarm"
  alarm_description         = "The alarm will be triggered if change handler dead letter queue messages received greater than threshold."
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.change_handler_dlq_threshold
  insufficient_data_actions = []
  metric_name               = "NumberOfMessagesReceived"
  namespace                 = "AWS/SQS"
  period                    = var.frontend_alarm_eval_period_sec
  statistic                 = "Sum"

  treat_missing_data = "notBreaching"

  dimensions = {
    QueueName = var.change_handler_dlq_sqs_name
  }
  alarm_actions = [var.frontend_sns_topic_arn]
}
