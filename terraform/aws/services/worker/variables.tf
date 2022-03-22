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

variable "instance_type" {
  type    = string
  default = "m5.2xlarge"
}

variable "key_name" {
  type    = string
  default = ""
}

variable "ami_name" {
  type        = string
  default     = "sample-enclaves-linux-aws"
  description = "AMI should contain an enclave image file (EIF)"
}

variable "ami_owners" {
  type        = list(string)
  default     = ["self"]
  description = "AWS accounts to check for worker AMIs."
}

variable "worker_template_name" {
  type        = string
  default     = "aggregation-service-template"
  description = "The name of worker aws launch template."
}

variable "ec2_iam_role_name" {
  description = "IAM role of the ec2 instance running the aggregation service."
  type        = string
  default     = "NewtonTestingEc2"
}

variable "ec2_instance_name" {
  description = "The name of each ec2 instance in the autoscaling group."
  type        = string
  default     = "aggregation-service-dev"
}

variable "service" {
  description = "Service name for aggregation service."
  type        = string
  default     = "aggregation-service"
}

variable "environment" {
  description = "Description for the environment, e.g. dev, staging, production."
  type        = string
}

variable "region" {
  description = "AWS region to deploy components"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR range for worker VPC."
  type        = string
  default     = "172.31.0.0/16"
}

variable "az_map" {
  description = "Availability zone to number mapping for dynamic subnet creation."
  default = {
    a = 1
    b = 2
    c = 3
    d = 4
    e = 5
    f = 6
  }
}

variable "coordinator_assume_role_arn" {
  description = "Role to assume when operating within the coordinator."
}

variable "metadata_db_table_arn" {
  description = "Name of the Dynamo DB table for storing job metadata."
}

variable "job_queue_arn" {
  description = "Name of the Dynamo DB table for storing job metadata."
}

variable "job_client_error_reasons" {
  type        = list(string)
  description = "Error reason for job client exception."
  default     = ["JOB_PULL_FAILED", "JOB_RECEIPT_HANDLE_NOT_FOUND", "JOB_METADATA_NOT_FOUND", "WRONG_JOB_STATUS", "JOB_MARK_COMPLETION_FAILED", "UNSPECIFIED_ERROR"]
}

variable "worker_alarms_enabled" {
  type        = string
  description = "Enable alarms for worker"
  default     = true
}

variable "job_client_error_threshold" {
  type        = number
  description = "Alarm threshold for job client errors."
  default     = 0
}

variable "job_validation_types" {
  type        = list(string)
  description = "Error reason for job client exception."
  default     = ["JobValidatorCheckDuplicate", "JobValidatorCheckFields", "JobValidatorCheckRetryLimit", "JobValidatorCheckStatus"]
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

variable "operator_sns_topic_arn" {
  type        = string
  description = "ARN for SNS topic to send alarm notifications"
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
