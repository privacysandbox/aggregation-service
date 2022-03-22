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
  description = "Environment name."
  type        = string
}

variable "region" {
  description = "AWS region to deploy service."
  type        = string
}

variable "instance_type" {
  description = "Parent EC2 instance type."
  type        = string
  default     = "c5.xlarge"
}

variable "key_name" {
  description = "Authorized key name."
  type        = string
  default     = ""
}

variable "ami_name" {
  description = "AMI name."
  type        = string
}

variable "ami_owners" {
  type        = list(string)
  default     = ["self"]
  description = "AWS accounts to check for worker AMIs."
}

variable "change_handler_lambda" {
  description = <<-EOT
        Change handler lambda path. If not provided defaults to locally built jar file.
      EOT
  type        = string
  default     = ""
}

variable "frontend_lambda" {
  description = <<-EOT
        Frontend lambda path. If not provided defaults to locally built jar file.
      EOT
  type        = string
  default     = ""
}

variable "sqs_write_failure_cleanup_lambda" {
  description = <<-EOT
        SQS write failure cleanup lambda path. If not provided defaults to locally built jar file. 
      EOT
  type        = string
  default     = ""
}

variable "max_job_num_attempts_parameter" {
  description = "Maximum number of times a job can be attempted for processing by a worker."
  type        = string
  default     = "5"
}

variable "max_job_processing_time_parameter" {
  description = "Maximum job processing time (Seconds)."
  type        = string
  default     = "3600"
}

variable "assume_role_parameter" {
  description = "Coordinator role ARN."
  type        = string
}

variable "kms_key_parameter" {
  description = "Coordinator KMS key ARN for testing outside of enclave."
  type        = string
  default     = "arn:aws:kms::example:key"
}

variable "initial_capacity_ec2_instances" {
  description = "Autoscaling initial capacity."
  type        = number
  default     = 1
}

variable "min_capacity_ec2_instances" {
  description = "Autoscaling min capacity."
  type        = number
  default     = 1
}

variable "max_capacity_ec2_instances" {
  description = "Autoscaling max capacity."
  type        = number
  default     = 20
}

variable "asg_capacity_handler_lambda" {
  description = <<-EOT
        ASG capacity handler lambda path. If not provided defaults to locally built
        jar file.
      EOT
  type        = string
  default     = ""
}

variable "terminated_instance_handler_lambda" {
  description = <<-EOT
        Terminated instance handler lambda path. If not provided defaults to locally built
        jar file.
      EOT
  type        = string
  default     = ""
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
