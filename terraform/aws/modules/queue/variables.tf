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

variable "environment" {
  type        = string
  description = "Description for the environment, e.g. dev, staging, production"
}

variable "region" {
  description = "AWS region to deploy components"
  type        = string
}

variable "queue_name" {
  type        = string
  description = "Name of the SQS queue to create."
}

variable "service_tag" {
  type        = string
  description = "Tag the queue with which service"
}

variable "role_tag" {
  type        = string
  description = "Tag the queue with which role"
}

variable "alarms_enabled" {
  type        = string
  description = "Enable alarms for queue"
}

variable "alarms_name" {
  type        = string
  description = "Name of queue alarm"
}

variable "queue_old_message_threshold_sec" {
  type        = number
  description = "Alarm threshold for old job queue messages in seconds."
  default     = 3600 //one hour
}

variable "queue_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 300s"
  type        = string
  default     = "300"
}

variable "sns_topic_arn" {
  type        = string
  description = "ARN for SNS topic to send alarm notifications"
}
