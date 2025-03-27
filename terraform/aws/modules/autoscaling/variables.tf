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

variable "asg_name" {
  type        = string
  default     = "aggregation-service-workers"
  description = "The name of auto scaling group."
}

variable "enable_autoscaling" {
  type    = bool
  default = true
}

variable "initial_capacity_ec2_instances" {
  description = "Initial capacity for ec2 instances. If autoscaling is not enabled, the number of instances will always equal this value."
  type        = number
  default     = 1
}

variable "worker_template_id" {
  type        = string
  description = "The worker template id to associate with the autoscaling group."
}

variable "worker_template_version" {
  type        = string
  description = "The worker template version to associate with the autoscaling group."
}

variable "max_ec2_instances" {
  description = "Upper bound for autoscaling for ec2 instances."
  type        = number
  default     = 20
}

variable "min_ec2_instances" {
  description = "Lower bound for autoscaling for ec2 instances."
  type        = number
  default     = 1
}

variable "lambda_package_storage_bucket_prefix" {
  type        = string
  description = "Prefix of the bucket to create and store lambda jars in"
}

variable "asg_capacity_handler_lambda_local_jar" {
  type        = string
  description = "Path to the jar for the ASG capacity handler lambda"
}

variable "asg_capacity_handler_lambda_name" {
  type        = string
  description = "Name for the ASG capacity handler lambda."
}

variable "asg_capacity_handler_method" {
  type        = string
  default     = "com.google.scp.operator.autoscaling.app.aws.AsgCapacityHandler"
  description = "Fully qualified method the ASG capacity handler lambda is called with"
}

variable "jobqueue_sqs_url" {
  type        = string
  description = "The URL of the SQS Queue to use as the JobQueue"
}

variable "jobqueue_sqs_arn" {
  type        = string
  description = "The resource arn of the SQS Queue to use as the JobQueue"
}

variable "asg_capacity_handler_memory_size" {
  type        = number
  default     = 1024
  description = "Amount of memory in MB to give the ASG capacity handler lambda"
}

variable "asg_capacity_handler_lambda_runtime_timeout" {
  type        = number
  default     = 30
  description = <<-EOT
  Timeout for the asg capacity handler lambda to run in seconds. This is
  the lambda runtime not the overall processing timeout.
  EOT
}

variable "asg_capacity_lambda_role_name" {
  type        = string
  description = "The name for the IAM role used by autoscaling capacity lambda"
}

variable "worker_scaling_ratio" {
  type        = number
  default     = 1
  description = "The ratio of worker instances to jobs (0.5: 1 worker / 2 jobs)"
  validation {
    condition     = var.worker_scaling_ratio > 0
    error_message = "Must be greater than 0."
  }
}

variable "worker_subnet_ids" {
  type        = list(string)
  description = "The subnet ids to launch instances in for the autoscaling group"
}

variable "terminated_instance_handler_lambda_local_jar" {
  type        = string
  description = "Path to the jar for the terminated instance handler lambda"
}

variable "terminated_instance_handler_lambda_name" {
  type        = string
  description = "Name for the terminated instance handler lambda."
}

variable "terminated_instance_handler_method" {
  type        = string
  default     = "com.google.scp.operator.autoscaling.app.aws.TerminatedInstanceHandler"
  description = "Fully qualified method the terminated instance handler lambda is called with"
}

variable "terminated_instance_handler_memory_size" {
  type        = number
  default     = 1024
  description = "Amount of memory in MB to give the terminated instance handler lambda"
}

variable "terminated_instance_handler_lambda_runtime_timeout" {
  type        = number
  default     = 30
  description = <<-EOT
  Timeout for the terminated instance handler lambda to run in seconds. This is
  the lambda runtime not the overall processing timeout.
  EOT
}

variable "terminated_instance_lambda_role_name" {
  type        = string
  description = "The name for the IAM role used by terminated instance lambda"
}

variable "termination_hook_heartbeat_timeout_sec" {
  type        = number
  description = <<-EOT
        Autoscaling termination lifecycle hook heartbeat timeout in seconds.
        If using termination hook timeout extension, this value is recommended
        to be greater than 10 minutes to allow heartbeats to occur. The max
        value for heartbeat is 7200 (2 hours) as per AWS documentation.
    EOT
}

variable "termination_hook_timeout_extension_enabled" {
  type        = bool
  description = <<-EOT
        Enable sending heartbeats to extend timeout for worker autoscaling
        termination lifecycle hook action. Required if the user wants to
        be able to wait over 2 hours for jobs to complete before instance
        termination.
     EOT
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
}

variable "termination_hook_max_timeout_extension_sec" {
  type        = number
  description = <<-EOT
        Max time to heartbeat the autoscaling termination lifecycle hook in
        seconds. The exact timeout could exceed this value since heartbeats
        increase the timeout by a fixed amount of time. Used if
        termination_hook_timeout_extension_enabled is true."
      EOT
}

################################################################################
# Terminated Instances Lambda Environment Variables
################################################################################

variable "asginstances_db_table_name" {
  type        = string
  description = "The name of the AsgInstances DynamoDB table."
}

variable "asginstances_db_arn" {
  type        = string
  description = "The ARN of the AsgInstances DynamoDB table."
}

variable "asginstances_db_ttl_days" {
  type        = number
  description = "The TTL in days for records in the AsgInstances DynamoDB table."
}

################################################################################
# Metric/Alarm Variables
################################################################################
variable "worker_alarms_enabled" {
  type        = string
  description = "Enable alarms for worker"
  default     = true
}

variable "operator_sns_topic_arn" {
  type        = string
  description = "ARN for SNS topic to send alarm notifications"
}

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
