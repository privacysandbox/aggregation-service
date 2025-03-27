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

variable "worker_ssh_public_key" {
  description = "RSA public key to be used for SSH access to worker EC2 instance."
  type        = string
  default     = ""
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type to use for aggregation workers."
  default     = "m5.xlarge" # 4 cores, 16GiB
}

variable "enclave_cpu_count" {
  description = "Number of CPUs to allocate to the enclave."
  type        = number
  default     = 2 # Leave 2 vCPUs to host OS (minimum required).  Depends on instance_type selected.
}

variable "enclave_memory_mib" {
  description = "Memory (in mebibytes) to allocate to the enclave."
  type        = number
  default     = 13312 # 13 GiB. ~15GiB are available on m5.xlarge, leave 2GiB for host OS.  Depends on instance_type selected.
}

variable "ami_name" {
  type        = string
  description = "Name of the AMI to deploy as aggregation workers."
}

variable "ami_owners" {
  type = list(string)
  # See https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ami#owners
  description = "Owners used to search for $ami_name."
  default     = ["self"]
}

variable "change_handler_lambda" {
  description = <<-EOT
        Change handler lambda path. If not provided defaults to tar location jar file.
        Build with `cd terraform/aws/jars/ && ./build_local_jars.sh`.
      EOT
  type        = string
  default     = "../../jars/AwsChangeHandlerLambda_deploy.jar"
}

variable "frontend_lambda" {
  description = <<-EOT
        Frontend lambda path. If not provided defaults to tar location jar file.
        Build with `cd terraform/aws/jars/ && ./build_local_jars.sh`.
      EOT
  type        = string
  default     = "../../jars/AwsApigatewayFrontend_deploy.jar"
}

variable "sqs_write_failure_cleanup_lambda" {
  description = <<-EOT
        SQS write failure cleanup lambda path. If not provided defaults to tar location jar file.
        Build with `cd terraform/aws/jars/ && ./build_local_jars.sh`.
      EOT
  type        = string
  default     = "../../jars/AwsFrontendCleanupLambda_deploy.jar"
}

# Deprecated and replaced by "coordinator_a_assume_role_parameter", used only for
# backwards compatibility
variable "assume_role_parameter" {
  type        = string
  description = "ARN of the role that the aggregation workers should assume."
  # TODO: Remove after migration to updated client.
  default = ""
}

variable "coordinator_a_assume_role_parameter" {
  type        = string
  description = "ARN of the role that the aggregation workers should assume to access coordinator A."
  # TODO: Remove after migration to updated client.
  default = ""
}

variable "coordinator_b_assume_role_parameter" {
  type        = string
  description = "ARN of the role that the aggregation workers should assume to access coordinator B."
  default     = ""
}

variable "kms_key_parameter" {
  type        = string
  description = "ARN of the AWS KMS key used for encryption/decryption for testing outside of enclave."
  default     = "arn:aws:kms::example:key"
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
# Autoscaling Variables
################################################################################

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
  default     = 20
}

variable "asg_capacity_handler_lambda" {
  description = <<-EOT
        ASG capacity handler lambda path. If not provided defaults to tar location jar file.
        Build with `cd terraform/aws/jars/ && ./build_local_jars.sh`.
      EOT
  type        = string
  default     = "../../jars/AsgCapacityHandlerLambda_deploy.jar"
}

variable "terminated_instance_handler_lambda" {
  description = <<-EOT
        Terminated instance handler lambda path. If not provided defaults to tar location jar file.
        Build with `cd terraform/aws/jars/ && ./build_local_jars.sh`.
      EOT
  type        = string
  default     = "../../jars/TerminatedInstanceHandlerLambda_deploy.jar"
}

variable "termination_hook_heartbeat_timeout_sec" {
  type        = number
  description = <<-EOT
        Autoscaling termination lifecycle hook heartbeat timeout in seconds.
        If using termination hook timeout extension, this value is recommended
        to be greater than 10 minutes to allow heartbeats to occur. The max
        value for heartbeat is 7200 (2 hours) as per AWS documentation.
    EOT
  default     = 3600
}

variable "termination_hook_timeout_extension_enabled" {
  type        = bool
  description = <<-EOT
        Enable sending heartbeats to extend timeout for worker autoscaling
        termination lifecycle hook action. Required if the user wants to
        be able to wait over 2 hours for jobs to complete before instance
        termination.
     EOT
  default     = true
}

variable "termination_hook_heartbeat_frequency_sec" {
  type        = number
  description = <<-EOT
        Autoscaling termination lifecycle hook heartbeat frequency in seconds.
        If using termination hook timeout extension, this value is recommended
        to be greater than 10 minutes to allow heartbeats to occur to avoid
        Autoscaling API throttling. The value should be less than
        termination_hook_heartbeat_timeout_sec to allow heartbeats to happen
        before the heartbeat timeout.
    EOT
  default     = 1800
}

variable "termination_hook_max_timeout_extension_sec" {
  type        = number
  description = <<-EOT
        Max time to heartbeat the autoscaling termination lifecycle hook in
        seconds. The exact timeout could exceed this value since heartbeats
        increase the timeout by a fixed amount of time. Used if
        termination_hook_timeout_extension_enabled is true."
      EOT
  default     = 3600
}

################################################################################
# MetadataDB Variables
################################################################################

variable "metadatadb_read_capacity" {
  type        = number
  description = "The read capacity units for the MetadataDB DynamoDB Table"
  default     = 5
}

variable "metadatadb_write_capacity" {
  type        = number
  description = "The write capacity units for the MetadataDB DynamoDB Table"
  default     = 5
}

################################################################################
# AsgInstancesDB Variables
################################################################################

variable "asginstances_db_ttl_days" {
  type        = number
  description = "The TTL in days for records in the AsgInstances DynamoDB table."
  default     = 7
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

################################################################################
# MetadataDB Alarm Variables
################################################################################

variable "metadatadb_alarm_eval_period_sec" {
  type        = string
  description = "Amount of time (in seconds) for alarm evaluation. Default 60"
  default     = "60"
}

variable "metadatadb_read_capacity_usage_ratio_alarm_threshold" {
  type        = string
  description = "Read capacity usage ratio greater than this to send alarm. Value should be decimal. Example: 0.9"
  default     = "0.9"
}

variable "metadatadb_write_capacity_usage_ratio_alarm_threshold" {
  type        = string
  description = "Write capacity usage ratio greater than this to send alarm. Value should be decimal. Example: 0.9"
  default     = "0.9"
}

################################################################################
# VPC, subnets and security group variables
################################################################################

variable "enable_user_provided_vpc" {
  description = "Allow providing existing vpc details as input."
  type        = bool
  default     = false
}

variable "user_provided_vpc_subnet_ids" {
  description = "Customized VPC subnet ids for worker autoscaling group"
  type        = list(string)
  default     = []
}

variable "user_provided_vpc_security_group_ids" {
  description = "Customized VPC security group ids for worker autoscaling group"
  type        = list(string)
  default     = []
}

variable "vpc_cidr" {
  description = "VPC CIDR range for coordinator VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "vpc_availability_zones" {
  description = "Specify the letter identifiers of which availability zones to deploy resources, such as a, b or c."
  type        = set(string)
  default = [
    "a",
    "b",
    "c",
    "d",
    "e",
  ]
}

################################################################################
# Notifications Variables.
################################################################################

variable "enable_job_completion_notifications" {
  description = "Determines if the SNS topic should be created for job completion notifications."
  type        = bool
  default     = false
}

################################################################################
# OpenTelemetry related variables
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

################################################################################
#  Per-coordinator config variables
################################################################################

variable "coordinator_configs" {
  type = map(object({
    gcp_project_number = string
    gcp_project_id     = string
    aws_account_id     = string
    env_name           = string
  }))
  default = {}

  description = <<EOT
      A map with per-coordinator config variables. Keys must be 'COORDINATOR_A' and 'COORDINATOR_B'.
      Values are objects with variables specific to each coordinator. Example:
      coordinator_configs = {
       "COORDINATOR_A" = {
         gcp_project_number = "12312312312"
         gcp_project_id = "project-a"
         aws_account_id = "23123123123"
         env_name = "prod"
       },
       "COORDINATOR_B" = {
         gcp_project_number = "78978978978"
         gcp_project_id = "project-b"
         aws_account_id = "987987987987"
         env_name = "prod"
       }
      }
    EOT
}

variable "coordinator_wif_config_override" {
  type = map(object({
    workload_identity_pool_id          = string
    workload_identity_pool_provider_id = string
    sa_email                           = string
  }))
  default = {}

  description = <<EOT
      Optional override for per-coordinator values used for Workload Identity Federation (WIF).
      If not provided, WIF values are generated automatically based on coordinator_config.
      Keys must be 'COORDINATOR_A' and 'COORIDNATOR_B'. Example:
      coordinator_wif_config_override = {
       "COORDINATOR_A" = {
         workload_identity_pool_id = "prod-aws-wip-1"
         workload_ideanity_pool_provider_id = "wipp-1"
         sa_email = "prod-fed-aws-1@12312312312.iam.gserviceaccount.com"
       },
       "COORDINATOR_B" = {
         workload_identity_pool_id = "prod-aws-wip-1"
         workload_ideanity_pool_provider_id = "wipp-1"
         sa_email = "prod-fed-aws-1@32132132132.iam.gserviceaccount.com"
       }
      }
  EOT
}
