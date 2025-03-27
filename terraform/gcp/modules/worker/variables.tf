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
  description = "Environment where this service is deployed (e.g. dev, prod)."
  type        = string
}

variable "project_id" {
  description = "project id"
  type        = string
}

################################################################################
# Network Variables.
################################################################################

variable "network" {
  description = "VPC Network name or self-link to use for worker."
  type        = string
}

variable "egress_internet_tag" {
  description = "Instance tag that grants internet access."
  type        = string
}

################################################################################
# Worker Variables.
################################################################################

variable "instance_type" {
  description = "GCE instance type for worker."
  type        = string
}

variable "instance_disk_image" {
  description = "The image from which to initialize the worker instance disk."
  type        = string
}

variable "worker_logging_enabled" {
  description = "Whether to enable worker logging."
  type        = bool
}

variable "worker_monitoring_enabled" {
  description = "Whether to enable worker monitoring."
  type        = bool
}

variable "worker_image" {
  description = "The worker docker image."
  type        = string
}

variable "worker_restart_policy" {
  description = "The TEE restart policy. Currently only supports Never"
  type        = string
}

variable "coordinator_a_impersonate_service_account" {
  description = "The service account provided by coordinator a for operator worker to impersonate."
  type        = string
}

variable "coordinator_b_impersonate_service_account" {
  description = "The service account provided by coordinator b for operator worker to impersonate."
  type        = string
}

variable "metadatadb_name" {
  description = "Name of the JobMetadata Spanner database."
  type        = string
}

variable "metadatadb_instance_name" {
  description = "Name of the TerminatedInstances Spanner instance."
  type        = string
}

variable "job_queue_sub" {
  description = "Name of the job queue subscription."
  type        = string
}

variable "job_queue_topic" {
  description = "Name of the job queue topic."
  type        = string
}

variable "user_provided_worker_sa_email" {
  description = "User provided service account email for worker."
  type        = string
}

variable "worker_instance_force_replace" {
  description = "Whether to force worker instance replacement for every deployment"
  type        = bool
}

################################################################################
# Monitoring Variables.
################################################################################

variable "autoscaler_cloudfunction_name" {
  description = "Name for the cloud function used in autoscaling worker VMs."
  type        = string
}

variable "autoscaler_name" {
  description = "Name of the autoscaler for the worker VM."
  type        = string
}

variable "vm_instance_group_name" {
  description = "Name for the instance group for the worker VM."
  type        = string
}

variable "alarms_enabled" {
  description = "Enable alarms for this service."
  type        = bool
}

variable "alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
  type        = string
}

variable "alarm_duration_sec" {
  description = "Amount of time (in seconds) after which to send alarm if conditions are met. Must be in minute intervals. Example: '60','120'."
  type        = string
}

variable "notification_channel_id" {
  description = "Notification channel to which to send alarms."
  type        = string
}

################################################################################
# Collector Variables
################################################################################

variable "collector_service_name" {
  description = "Name of the collector service."
  type        = string
}

variable "collector_machine_type" {
  description = "Machine type for the collector service."
  type        = string
}

variable "collector_service_port" {
  description = "The grpc port that receives traffic destined for the OpenTelemetry collector."
  type        = number
}

variable "vm_startup_delay_seconds" {
  description = "The time it takes to get a service up and responding to heartbeats (in seconds)."
  type        = number
}

variable "max_collectors_per_region" {
  description = "Maximum amount of Collectors per each service region (a single managed instance group)."
  type        = number
}

variable "target_cpu_utilization" {
  description = "CPU utilization fraction [range 0.0 - 1.0] across an instance group required for autoscaler to add instances."
  type        = number
}

variable "collector_startup_script" {
  description = "Script thats starts the OTel collector."
  type        = string
}

################################################################################
# OTel related variables
################################################################################

variable "allowed_otel_metrics" {
  description = "Set of otel metrics to be exported."
  type        = set(string)
  default     = []
}

variable "min_log_level" {
  description = "Minimum log level to export. No logs will be exported if it's empty string."
  type        = string
  default     = ""
  validation {
    condition     = contains(["", "INFO", "WARN", "ERROR"], var.min_log_level)
    error_message = "The values should be one of ['', 'INFO', 'WARN', 'ERROR']."
  }
}
