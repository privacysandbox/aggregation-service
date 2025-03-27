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
  description = "Description for the environment, e.g. dev, staging, production"
  type        = string
}

variable "region" {
  description = "Region where all services will be created."
  type        = string
}

variable "region_zone" {
  description = "Region zone where all services will be created."
  type        = string
}

variable "operator_package_bucket_location" {
  description = "Location for operator packages. Example: 'US'"
  type        = string
}

################################################################################
# Global Alarm Variables.
################################################################################

variable "alarms_enabled" {
  description = "Enable alarms for services."
  type        = bool
  default     = false
}

variable "alarms_notification_email" {
  description = "Email to receive alarms for services."
  type        = string
  default     = ""
}

################################################################################
# Common Spanner Variables.
################################################################################

variable "spanner_instance_config" {
  description = "Config value for the Spanner Instance"
  type        = string
}

variable "spanner_processing_units" {
  description = "Number of processing units allocated to the jobmetadata instance. 1000 processing units = 1 node and must be set as a multiple of 100."
  type        = number
  default     = 1000
}

variable "spanner_database_deletion_protection" {
  description = "Prevents destruction of the Spanner database."
  type        = bool
  default     = true
}

################################################################################
# Worker Variables.
################################################################################

variable "instance_type" {
  description = "GCE instance type for worker."
  type        = string
  default     = "n2d-standard-2"
}

variable "instance_disk_image" {
  description = "The image from which to initialize the worker instance disk."
  type        = string
  default     = "confidential-space-images/confidential-space-debug"
}

variable "worker_logging_enabled" {
  description = "Whether to enable worker logging."
  type        = bool
  default     = false
}

variable "worker_monitoring_enabled" {
  description = "Whether to enable worker monitoring."
  type        = bool
  default     = true
}

variable "worker_image" {
  description = "The worker docker image."
  type        = string
}

variable "worker_restart_policy" {
  description = "The TEE restart policy. Currently only supports Never"
  type        = string
  default     = "Never"
}

variable "allowed_operator_service_account" {
  description = "The service account provided by coordinator for operator worker to impersonate."
  type        = string
}

variable "user_provided_worker_sa_email" {
  description = "User provided service account email for worker."
  type        = string
  default     = ""
}

variable "worker_instance_force_replace" {
  description = "Whether to force worker instance replacement for every deployment"
  type        = bool
  default     = false
}

variable "worker_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
  type        = string
  default     = "60"
}

variable "worker_alarm_duration_sec" {
  description = "Amount of time (in seconds) after which to send alarm if conditions are met. Must be in minute intervals. Example: '60','120'."
  type        = string
  default     = "60"
}

################################################################################
# Autoscaling Variables.
################################################################################

variable "min_worker_instances" {
  description = "Minimum number of instances in worker managed instance group."
  type        = number
  default     = 1
}

variable "max_worker_instances" {
  description = "Maximum number of instances in worker managed instance group."
  type        = number
  default     = 20
}

variable "autoscaling_jobs_per_instance" {
  description = "The ratio of jobs to worker instances to scale by."
  type        = number
  default     = 1
}

################################################################################
# Autoscaling Alarm Variables.
################################################################################

variable "autoscaling_alarm_eval_period_sec" {
  description = "Time period (in seconds) for alarm evaluation."
  type        = string
  default     = "60"
}

variable "autoscaling_alarm_duration_sec" {
  description = "Amount of time (in seconds) to wait before sending an alarm. Must be in minute intervals. Example: '60','120'."
  type        = string
  default     = "60"
}

variable "autoscaling_max_vm_instances_ratio_threshold" {
  description = "A decimal ratio of current to maximum allowed VMs, exceeding which sends an alarm. Example: 0.9."
  type        = number
  default     = 0.9
}

################################################################################
# Job Queue Alarm Variables.
################################################################################

variable "jobqueue_alarm_eval_period_sec" {
  description = "Time period (in seconds) for alarm evaluation."
  type        = string
  default     = "60"
}

variable "jobqueue_max_undelivered_message_age_sec" {
  description = "Maximum time (in seconds) to wait for message delivery before triggering alarm."
  type        = number
  default     = 60 * 60
}

################################################################################
# VPC Service Control Variables.
################################################################################

variable "vpcsc_compatible" {
  description = <<EOT
      Enable VPC Service Control compatible features:
      * Serverless VPC Access connectors for all Cloud Function functions.
    EOT
  type        = bool
  default     = false
}

variable "vpc_connector_machine_type" {
  description = "Machine type of Serverless VPC Access connectors."
  type        = string
  default     = "e2-micro"
}


################################################################################
# Frontend Service Cloud Function Variables.
################################################################################
variable "frontend_service_jar" {
  description = <<-EOT
          Get frontend service cloud function path. If not provided defaults to locally built jar file.
        Build with `bazel build //terraform/gcp/applications/jobservice:all`.
      EOT
  type        = string
  default     = null
}

variable "frontend_service_cloudfunction_memory_mb" {
  description = "Memory size in MB for frontend service cloud function."
  type        = number
  default     = 1024
}

variable "frontend_service_cloudfunction_min_instances" {
  description = "The minimum number of frontend function instances that may coexist at a given time.."
  type        = number
  default     = 0
}

variable "frontend_service_cloudfunction_max_instances" {
  description = "The maximum number of frontend function instances that may coexist at a given time.."
  type        = number
  default     = 100
}

variable "frontend_service_cloudfunction_timeout_sec" {
  description = "Number of seconds after which a frontend function instance times out."
  type        = number
  default     = 60
}

variable "job_table_ttl_days" {
  description = "The number of days to retain job table records."
  type        = number
  validation {
    condition     = var.job_table_ttl_days > 0
    error_message = "Must be greater than 0."
  }
  default = 365
}

variable "job_version" {
  description = "The version of frontend service. Version 2 supports new job schema from C++ CMRT library."
  type        = string
  default     = "2"
}

################################################################################
# Frontend Service Alarm Variables.
################################################################################

variable "frontend_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
  type        = string
  default     = "60"
}

variable "frontend_alarm_duration_sec" {
  description = "Amount of time (in seconds) after which to send alarm if conditions are met. Must be in minute intervals. Example: '60','120'."
  type        = string
  default     = "60"
}

variable "frontend_cloudfunction_error_threshold" {
  description = "Error count greater than this to send alarm. Example: 0."
  type        = string
  default     = "0"
}

variable "frontend_cloudfunction_max_execution_time_max" {
  description = "Max execution time in ms to send alarm. Example: 9999."
  type        = string
  default     = "5000"
}

variable "frontend_cloudfunction_5xx_threshold" {
  description = "Cloud Function 5xx error count greater than this to send alarm. Example: 0."
  type        = string
  default     = "0"
}

variable "frontend_lb_max_latency_ms" {
  description = "Load Balancer max latency to send alarm. Measured in milliseconds. Example: 5000."
  type        = string
  default     = "5000"
}

variable "frontend_lb_5xx_threshold" {
  description = "Load Balancer 5xx error count greater than this to send alarm. Example: 0."
  type        = string
  default     = "0"
}
