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
  description = "Description for the environment, e.g. dev, staging, production."
  type        = string
}

variable "notification_channel_id" {
  description = "Notification channel to which to send alarms."
  type        = string
}

variable "function_name" {
  description = "Name of cloud function for which to create alarms."
  type        = string
}

variable "service_prefix" {
  description = "Prefix to use in alert Display Name. Should contain environment and region."
  type        = string
}

################################################################################
# Alarm Variables.
################################################################################

variable "eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
  type        = string
}

variable "duration_sec" {
  description = "Amount of time (in seconds) after which to send alarm if conditions are met. Must be in minute intervals. Example: '60','120'."
  type        = string
}

variable "error_5xx_threshold" {
  description = "5xx error count greater than this to send alarm. Example: 0."
  type        = number
}

variable "execution_error_threshold" {
  description = "Execution error count greater than this to send alarm. Example: 0."
  type        = number
}

variable "execution_time_max" {
  description = "Max latency to send alarm. Measured in milliseconds. Example: 5000."
  type        = number
}
