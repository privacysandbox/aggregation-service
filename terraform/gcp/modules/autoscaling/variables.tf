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

variable "initial_worker_instances" {
  description = "Initial number of instances in worker managed instance group."
  type        = number
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

variable "autoscaling_cloudfunction_memory_mb" {
  description = "Memory size in MB for autoscaling cloud function."
  type        = number
}

variable "worker_service_account" {
  description = "The service account to deploy services."
  type        = string
}

variable "operator_package_bucket_name" {
  description = "Name of bucket containing cloud function jars."
  type        = string
}

variable "worker_scale_in_jar" {
  description = "Path to the jar file for lambda function."
  type        = string
}

variable "metadatadb_instance_name" {
  description = "The metadata database Spanner instance name."
  type        = string
}

variable "metadatadb_name" {
  description = "The metadata database Spanner database name."
  type        = string
}

variable "termination_wait_timeout_sec" {
  description = <<-EOT
    The instance termination timeout before force terminating (seconds). The value
    should be greater than max_job_processing_time to ensure jobs can complete in
    before instance termination.
  EOT
  type        = string
}

variable "worker_scale_in_cron" {
  description = "The cron schedule for the worker scale-in scheduler."
  type        = string
}


variable "asg_instances_table_ttl_days" {
  description = "The number of days to retain AsgInstances table records."
  type        = number
  validation {
    condition     = var.asg_instances_table_ttl_days > 0
    error_message = "Must be greater than 0."
  }
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

variable "cloudfunction_error_threshold" {
  description = "Error counts greater than this value will send an alarm."
  type        = number
}

variable "cloudfunction_max_execution_time_ms" {
  description = "Max execution time (in ms) allowed before sending an alarm. Example: 9999."
  type        = number
}

variable "cloudfunction_5xx_threshold" {
  description = "5xx error counts greater than this value will send an alarm."
  type        = number
}

variable "max_vm_instances_ratio_threshold" {
  description = "A decimal ratio of current to maximum allowed VMs, exceeding which sends an alarm. Example: 0.9."
  type        = number
}
