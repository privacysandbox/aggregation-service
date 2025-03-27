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

variable "environment" {
  type        = string
  description = "Description for the environment, e.g. dev, staging, production"
}

variable "table_name" {
  type        = string
  description = "Name of the DynamoDB table, should include environment"
}

variable "write_capacity" {
  type        = number
  description = "Provisioned write capacity units"
}

variable "read_capacity" {
  type        = number
  description = "Provisioned read capacity units"
}

variable "enable_dynamo_point_in_time_recovery" {
  description = "Allows DynamoDB table data to be recovered after table deletion"
  type        = bool
  default     = true
}

variable "worker_alarms_enabled" {
  type        = string
  description = "Enable alarms for worker"
}

variable "sns_topic_arn" {
  type        = string
  description = "SNS topic ARN to forward alerts to"
}

variable "eval_period_sec" {
  type        = string
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
}

variable "metadatadb_read_capacity_usage_ratio_alarm_threshold" {
  type        = string
  description = "The capacity limit of metadatadb table read processing unit"
}

variable "metadatadb_write_capacity_usage_ratio_alarm_threshold" {
  description = "The capacity limit of metadatadb table write processing unit"
  type        = string
}
