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

variable "worker_ssh_public_key" {
  description = "RSA public key to be used for SSH access to worker EC2 instance."
  type        = string
}

variable "ami_name" {
  description = "AMI should contain an enclave image file (EIF)."
  type        = string
  default     = "sample-enclaves-linux-aws"
}

variable "ami_owners" {
  description = "AWS accounts to check for worker AMIs."
  type        = list(string)
  default     = ["self"]
}

variable "worker_template_name" {
  description = "The name of worker aws launch template."
  type        = string
  default     = "aggregation-service-template"
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
  description = "AWS region to deploy components."
  type        = string
}

variable "coordinator_a_assume_role_arn" {
  description = "Role to assume when operating within Coordinator A."
}

variable "coordinator_b_assume_role_arn" {
  description = "Role to assume when operating within Coordinator B."
}

variable "metadata_db_table_arn" {
  description = "Name of the Dynamo DB table for storing job metadata."
}

variable "asg_instances_table_arn" {
  description = "ARN of the Dynamo DB table for storing instance termination records."
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
  description = "Enable alarms for worker."
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
  description = "ARN for SNS topic to send alarm notifications."
}

variable "worker_alarm_eval_period_sec" {
  description = "Amount of time (in seconds) for alarm evaluation. Default 300s."
  type        = string
  default     = "300"
}

variable "worker_alarm_metric_dimensions" {
  description = "Metric dimensions for worker alarms."
  type        = list(string)
  default     = ["JobHandlingError"]
}

variable "worker_security_group_ids" {
  description = "IDs of the security groups used by worker enclave."
  type        = list(string)
}

variable "dynamodb_vpc_endpoint_id" {
  description = "ID of the VPC endpoint to access DynamoDb."
  type        = string
}

variable "s3_vpc_endpoint_id" {
  description = "ID of the VPC endpoint to access S3."
  type        = string
}

variable "enclave_cpu_count" {
  description = "Number of CPUs to allocate to the enclave."
  type        = number

  validation {
    condition     = var.enclave_cpu_count >= 2
    error_message = "Must allocate at least 2 vCPUs"
  }

  validation {
    condition     = var.enclave_cpu_count % 2 == 0
    error_message = "Must allocate an even number of vCPUs"
  }
}

variable "enclave_memory_mib" {
  description = "Memory (in mebibytes) to allocate to the enclave."
  type        = number

  validation {
    condition     = var.enclave_memory_mib >= 1024
    error_message = "Must allocate at least 1024 mebibytes."
  }
}

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
