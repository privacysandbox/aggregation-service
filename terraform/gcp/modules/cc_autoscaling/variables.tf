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

################################################################################
# Global Variables.
################################################################################

variable "project_id" {
  description = "GCP Project ID in which this module will be created."
  type        = string
}

variable "environment" {
  description = "Environment where this service is deployed (e.g. dev, prod)."
  type        = string
}

variable "region" {
  description = "Region where resources will be created."
  type        = string
}

variable "vpc_connector_id" {
  description = "Serverless VPC Access connector ID to use for all egress traffic."
  type        = string
  default     = null
}

################################################################################
# Autoscaling Variables.
################################################################################

variable "worker_template" {
  description = "The worker google_compute_instance_template."
  type        = any
}

variable "min_worker_instances" {
  description = "Minimum number of instances in worker managed instance group."
  type        = number
}

variable "max_worker_instances" {
  description = "Maximum number of instances in worker managed instance group."
  type        = number
}

variable "jobqueue_subscription_name" {
  description = "Subscription name of the job queue."
  type        = string
}

variable "autoscaling_jobs_per_instance" {
  description = "The ratio of jobs to worker instances to scale by."
  type        = number
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

variable "alarm_duration_sec" {
  description = "Amount of time (in seconds) to wait before sending an alarm. Must be in minute intervals. Example: '60','120'."
  type        = string
}

variable "max_vm_instances_ratio_threshold" {
  description = "A decimal ratio of current to maximum allowed VMs, exceeding which sends an alarm. Example: 0.9."
  type        = number
}
