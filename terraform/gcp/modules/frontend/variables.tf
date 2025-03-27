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

variable "job_version" {
  description = "The version of frontend service. Version 2 supports new job schema from C++ CMRT library."
  type        = string
}

################################################################################
# Cloud Function Variables.
################################################################################

variable "operator_package_bucket_name" {
  description = "Name of bucket containing cloudfunction jar."
  type        = string
}

variable "frontend_service_jar" {
  description = "Path to the jar file for cloudfunction."
  type        = string
}

variable "frontend_service_cloudfunction_memory_mb" {
  description = "Memory size in MB for cloudfunction."
  type        = number
}

variable "frontend_service_cloudfunction_min_instances" {
  description = "The minimum number of frontend function instances that may coexist at a given time.."
  type        = number
}

variable "frontend_service_cloudfunction_max_instances" {
  description = "The maximum number of frontend function instances that may coexist at a given time.."
  type        = number
}

variable "frontend_service_cloudfunction_timeout_sec" {
  description = "Number of seconds after which a frontend function instance times out."
  type        = number
}

################################################################################
# Spanner Variables.
################################################################################

variable "spanner_database_name" {
  description = "Name of the JobMetadata Spanner database."
  type        = string
}

variable "spanner_instance_name" {
  description = "Name of the JobMetadata Spanner instance."
  type        = string
}

variable "job_metadata_table_ttl_days" {
  description = "The number of days to retain JobMetadata table records."
  type        = number
  validation {
    condition     = var.job_metadata_table_ttl_days > 0
    error_message = "Must be greater than 0."
  }
}

################################################################################
# Queue Variables.
################################################################################

variable "job_queue_topic" {
  description = "Name of the job queue topic."
  type        = string
}

variable "job_queue_sub" {
  description = "Name of the job queue subscription."
  type        = string
}

################################################################################
# Alarm Variables.
################################################################################

variable "alarms_enabled" {
  description = "Enable alarms for this service."
  type        = bool
}

variable "notification_channel_id" {
  description = "Notification channel to which to send alarms."
  type        = string
}

variable "alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
  type        = string
}

variable "alarm_duration_sec" {
  description = "Amount of time (in seconds) after which to send alarm if conditions are met. Must be in minute intervals. Example: '60','120'."
  type        = string
}

variable "cloudfunction_error_threshold" {
  description = "Error count greater than this to send alarm. Example: 0."
  type        = string
}

variable "cloudfunction_max_execution_time_max" {
  description = "Max execution time in ms to send alarm. Example: 9999."
  type        = string
}

variable "cloudfunction_5xx_threshold" {
  description = "Cloud Function 5xx error count greater than this to send alarm. Example: 0."
  type        = string
}

variable "lb_max_latency_ms" {
  description = "Load Balancer max latency to send alarm. Measured in milliseconds. Example: 5000."
  type        = string
}

variable "lb_5xx_threshold" {
  description = "Load Balancer 5xx error count greater than this to send alarm. Example: 0."
  type        = string
}
