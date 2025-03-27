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

resource "aws_cloudwatch_metric_alarm" "queue_old_message_alarm" {
  count                     = var.alarms_enabled ? 1 : 0
  alarm_name                = "${var.environment}_${var.region}_${var.alarm_name}"
  alarm_description         = "SQS messages that have not completed processing in ${var.queue_old_message_threshold_sec} seconds"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = 1
  threshold                 = var.queue_old_message_threshold_sec
  insufficient_data_actions = []
  metric_name               = "ApproximateAgeOfOldestMessage"
  namespace                 = "AWS/SQS"
  period                    = var.queue_alarm_eval_period_sec
  statistic                 = "Maximum"

  treat_missing_data = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.queue.name
  }
  alarm_actions = [var.sns_topic_arn]
}
