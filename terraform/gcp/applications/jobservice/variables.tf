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
}

variable "alarms_notification_email" {
  description = "Email to receive alarms for services."
  type        = string
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
}

variable "spanner_database_deletion_protection" {
  description = "Prevents destruction of the Spanner database."
  type        = bool
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
}

variable "frontend_service_cloudfunction_memory_mb" {
  description = "Memory size in MB for frontend service cloud function."
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

variable "job_version" {
  description = "The version of frontend service. Version 2 supports new job schema from C++ CMRT library."
  type        = string
}

################################################################################
# Frontend Service Alarm Variables.
################################################################################

variable "frontend_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
  type        = string
}

variable "frontend_alarm_duration_sec" {
  description = "Amount of time (in seconds) after which to send alarm if conditions are met. Must be in minute intervals. Example: '60','120'."
  type        = string
}

variable "frontend_cloudfunction_error_threshold" {
  description = "Error count greater than this to send alarm. Example: 0."
  type        = string
}

variable "frontend_cloudfunction_max_execution_time_max" {
  description = "Max execution time in ms to send alarm. Example: 9999."
  type        = string
}

variable "frontend_cloudfunction_5xx_threshold" {
  description = "Cloud Function 5xx error count greater than this to send alarm. Example: 0."
  type        = string
}

variable "frontend_lb_max_latency_ms" {
  description = "Load Balancer max latency to send alarm. Measured in milliseconds. Example: 5000."
  type        = string
}

variable "frontend_lb_5xx_threshold" {
  description = "Load Balancer 5xx error count greater than this to send alarm. Example: 0."
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

variable "max_job_processing_time" {
  description = "Maximum job processing time (Seconds)."
  type        = string
}

variable "max_job_num_attempts" {
  description = "Max number of times a job can be picked up by a worker and attempted processing"
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

variable "worker_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Example: '60'."
  type        = string
}

variable "worker_alarm_duration_sec" {
  description = "Amount of time (in seconds) after which to send alarm if conditions are met. Must be in minute intervals. Example: '60','120'."
  type        = string
}

################################################################################
# Autoscaling Variables.
################################################################################

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

variable "autoscaling_jobs_per_instance" {
  description = "The ratio of jobs to worker instances to scale by."
  type        = number
}

variable "autoscaling_cloudfunction_memory_mb" {
  description = "Memory size in MB for autoscaling cloud function."
  type        = number
}

variable "worker_scale_in_jar" {
  description = <<-EOT
          Get worker scale in cloud function path. If not provided defaults to locally built jar file.
        Build with `bazel build //terraform/gcp/applications/jobservice:all`.
      EOT
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
# Autoscaling Alarm Variables.
################################################################################

variable "autoscaling_alarm_eval_period_sec" {
  description = "Time period (in seconds) for alarm evaluation."
  type        = string
}

variable "autoscaling_alarm_duration_sec" {
  description = "Amount of time (in seconds) to wait before sending an alarm. Must be in minute intervals. Example: '60','120'."
  type        = string
}

variable "autoscaling_max_vm_instances_ratio_threshold" {
  description = "A decimal ratio of current to maximum allowed VMs, exceeding which sends an alarm. Example: 0.9."
  type        = number
}

variable "autoscaling_cloudfunction_5xx_threshold" {
  description = "5xx error counts greater than this value will send an alarm."
  type        = number
}

variable "autoscaling_cloudfunction_error_threshold" {
  description = "Error counts greater than this value will send an alarm."
  type        = number
}

variable "autoscaling_cloudfunction_max_execution_time_ms" {
  description = "Max execution time (in ms) allowed before sending an alarm. Example: 9999."
  type        = number
}

################################################################################
# Job Queue Alarm Variables.
################################################################################

variable "jobqueue_alarm_eval_period_sec" {
  description = "Time period (in seconds) for alarm evaluation."
  type        = string
}

variable "jobqueue_max_undelivered_message_age_sec" {
  description = "Maximum time (in seconds) to wait for message delivery before triggering alarm."
  type        = number
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
}

variable "vpc_connector_machine_type" {
  description = "Machine type of Serverless VPC Access connectors."
  type        = string
}

################################################################################
# Notifications Variables.
################################################################################

variable "enable_job_completion_notifications" {
  description = "Determines if the Pub/Sub topic should be created for job completion notifications."
  type        = bool
}

################################################################################
# Collector Variables
################################################################################

variable "collector_service_name" {
  description = "Name of the collector service."
  type        = string
}

variable "collector_service_port" {
  description = "The grpc port that receives traffic destined for the OpenTelemetry collector."
  type        = number
}

variable "collector_machine_type" {
  description = "Machine type for the collector service."
  type        = string
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
