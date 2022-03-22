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

########################
# DO NOT EDIT MANUALLY #
########################

# This file is meant to be shared across all environments (either copied or
# symlinked). In order to make the upgrade process easier, this file should not
# be modified for environment-specific customization.

# Customization should occur either in a .tfvars file setting variable values or
# in a separate .tf file.

variable "region" {
  type        = string
  description = "Region to deploy services to. Used for resource names."
}

variable "environment" {
  type        = string
  description = "Name of the deployment environment (e.g. 'staging' or 'prod'). Used for resource names"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type to use for aggregation workers."
}

variable "ami_name" {
  type        = string
  description = "Name of the AMI to deploy as aggregation workers."
}

variable "ami_owners" {
  type = list(string)
  # See https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ami#owners
  description = "Owners used to search for $ami_name."
}

variable "assume_role_parameter" {
  type        = string
  description = "ARN of the role that the aggregation workers should assume."
}

variable "kms_key_parameter" {
  type        = string
  description = "ARN of the AWS KMS key used for encryption/decryption for testing outside of enclave."
  default     = "arn:aws:kms::example:key"
}

variable "initial_capacity_ec2_instances" {
  default     = 2
  type        = number
  description = "configuration for the aggregation worker autoscaling process."
}

variable "min_capacity_ec2_instances" {
  default     = 1
  type        = number
  description = "configuration for the aggregation worker autoscaling process."
}

variable "max_capacity_ec2_instances" {
  type        = number
  description = "configuration for the aggregation worker autoscaling process."
}

variable "max_job_num_attempts_parameter" {
  type        = string
  default     = "5"
  description = "Max number of times a job can be picked up by a worker and attempted processing"
}

variable "max_job_processing_time_parameter" {
  type        = string
  default     = "3600"
  description = "Max job processing time in seconds (queue visibility time-out)"
}

################################################################################
# Frontend Alarm Variables
################################################################################

variable "frontend_alarms_enabled" {
  type        = string
  description = "Enable alarms for frontend"
  default     = true
}

variable "frontend_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 60s"
  type        = string
  default     = "60"
}

variable "frontend_lambda_error_threshold" {
  description = "Error rate greater than this to send alarm. Must be in decimal form. Eg: 10% = 0.10. Default 0"
  type        = string
  default     = "0.0"
}

variable "frontend_lambda_max_throttles_threshold" {
  description = "Lambda max throttles to send alarm.  Default 10."
  type        = string
  default     = "10"
}

variable "frontend_lambda_max_duration_threshold" {
  description = "Lambda max duration in ms to send alarm. Useful for timeouts. Default 9999ms since lambda time out is set to 10s"
  type        = string
  default     = "9999"
}

variable "frontend_api_max_latency_ms" {
  description = "API Gateway max latency to send alarm. Measured in milliseconds. Default 5000ms"
  type        = string
  default     = "5000"
}

variable "frontend_5xx_threshold" {
  description = "API Gateway 5xx error rate greater than this to send alarm. Must be in decimal form. Eg: 10% = 0.10. Default 0"
  type        = string
  default     = "0.0"
}

variable "frontend_4xx_threshold" {
  description = "API Gateway 4xx error rate greater than this to send alarm. Must be in decimal form. Eg: 10% = 0.10. Default 0"
  type        = string
  default     = "0.0"
}

variable "change_handler_dlq_threshold" {
  description = "Change handler dead letter queue messages received greater than this to send alarm. Must be in decimal form. Eg: 10% = 0.10. Default 0"
  type        = string
  default     = "0.0"
}

variable "change_handler_max_latency_ms" {
  description = "Change handler max duration in ms to send alarm. Useful for timeouts. Default 9999ms since lambda time out is set to 10s"
  type        = string
  default     = "9999"
}

################################################################################
# Worker Alarm Shared Variables
################################################################################

variable "worker_alarms_enabled" {
  type        = string
  description = "Enable alarms for worker (includes alarms for autoscaling/jobqueue/worker)"
  default     = true
}

variable "alarm_notification_email" {
  description = "Email to send operator component alarm notifications"
  type        = string
  default     = "noreply@example.com"
}

################################################################################
# Autoscaling Alarm Variables
################################################################################

variable "asg_capacity_lambda_error_threshold" {
  type        = number
  default     = 0.0
  description = "Error rate greater than this to send alarm."
}

variable "asg_capacity_lambda_duration_threshold" {
  type        = number
  default     = 15000
  description = "Alarm duration threshold in msec for runs of the AsgCapacityHandler Lambda function."
}

variable "asg_capacity_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 300s"
  type        = string
  default     = "300"
}

variable "terminated_instance_lambda_error_threshold" {
  type        = number
  default     = 0.0
  description = "Error rate greater than this to send alarm."
}

variable "terminated_instance_lambda_duration_threshold" {
  type        = number
  default     = 15000
  description = "Alarm duration threshold in msec for runs of the TerminatedInstanceHandler Lambda function."
}

variable "terminated_instance_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 300s"
  type        = string
  default     = "300"
}

variable "asg_max_instances_alarm_ratio" {
  type        = number
  description = "Ratio of the auto scaling group max instances that should alarm on."
  default     = 0.9
}

variable "autoscaling_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 60s"
  type        = string
  default     = "60"
}

################################################################################
# Worker Alarm Variables
################################################################################

variable "job_client_error_threshold" {
  type        = number
  description = "Alarm threshold for job client errors."
  default     = 0
}

variable "job_validation_failure_threshold" {
  type        = number
  description = "Alarm threshold for job validation failures."
  default     = 0
}

variable "worker_job_error_threshold" {
  type        = number
  description = "Alarm threshold for worker job errors."
  default     = 0
}

variable "worker_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 300s"
  type        = string
  default     = "300"
}

variable "worker_alarm_metric_dimensions" {
  description = "Metric dimensions for worker alarms"
  type        = list(string)
  default     = ["JobHandlingError"]
}

################################################################################
# Job Queue Alarm Variables
################################################################################

variable "job_queue_old_message_threshold_sec" {
  type        = number
  description = "Alarm threshold for old job queue messages in seconds."
  default     = 3600 //one hour
}

variable "job_queue_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 300s"
  type        = string
  default     = "300"
}
