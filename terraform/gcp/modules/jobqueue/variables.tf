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
# Global Variables.
################################################################################

variable "environment" {
  type        = string
  description = "Environment where this service is deployed (e.g. dev, prod)."
}

################################################################################
# Alarm Variables.
################################################################################

variable "alarms_enabled" {
  description = "Enable alarms."
  type        = bool
}

variable "notification_channel_id" {
  description = "Email to receive alarms."
  type        = string
}

variable "alarm_eval_period_sec" {
  description = "Time period (in seconds) for alarm evaluation."
  type        = string
}

variable "max_undelivered_message_age_sec" {
  description = "Maximum time (in seconds) to wait for message delivery before triggering alarm."
  type        = number
}
