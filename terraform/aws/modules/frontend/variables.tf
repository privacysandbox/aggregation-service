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
  type        = string
  description = "Description for the environment, e.g. dev, staging, production"
}

variable "frontend_api_name" {
  type        = string
  default     = "frontend_api"
  description = "Name of the api gateway resource."
}

variable "lambda_package_storage_bucket_prefix" {
  type        = string
  description = "Prefix of the bucket to create and store lambda jars in"
}

variable "change_handler_lambda_name" {
  type        = string
  description = "Name for the change handler lambda."
}

variable "change_handler_lambda_role_name" {
  type        = string
  description = "Name for the change handler lambda's IAM role"
}

variable "change_handler_lambda_local_jar" {
  type        = string
  description = "Path to the jar for the change handler lambda"
}

variable "change_handler_method" {
  type        = string
  default     = "com.google.scp.operator.frontend.service.aws.DynamoStreamsJobMetadataHandler"
  description = "Fully qualified method the change handler lambda is called with"
}

variable "change_handler_memory_size" {
  type        = number
  default     = 2048
  description = "Amount of memory in MB to give the change handler lambda"
}

variable "change_handler_lambda_runtime_timeout" {
  type        = number
  default     = 15
  description = <<-EOT
  Timeout for the change handler lambda to run in seconds. This is the lambda
  runtime not the overall processing timeout.
  EOT
}

variable "change_handler_lambda_maximum_retry_attempts" {
  type        = number
  default     = 10
  description = <<-EOT
  The number of lambda executions for the change handler to put a job on the
  processing queue, when this limit is reached the item will be placed on the
  DLQ for cleanup.
  EOT
}

variable "change_handler_lambda_maximum_maximum_record_age_in_seconds" {
  type = number
  # 5 minutes
  default     = 300
  description = <<-EOT
  The maximum age of a job that the change handler will process, when a job
  reaches this age without being put on the processing queue, it will be placed
  on the DLQ for cleanup.
  EOT
}

variable "change_handler_dlq_sqs_name" {
  type        = string
  description = "The name the change handler's SQS DLQ will be created with"
}

variable "change_handler_dlq_sqs_message_retention_seconds" {
  type = number
  # 14 days
  default     = 1209600
  description = <<-EOT
  Message retention time for the SQS DLQ. Defaults to the max allowed by AWS
  (14 days)
  EOT
}

variable "cleanup_lambda_local_jar" {
  type        = string
  description = "Path to the jar for the cleanup lambda"
}

variable "cleanup_lambda_name" {
  type        = string
  description = "Name to use for the cleanup lambda"
}

variable "cleanup_lambda_role_name" {
  type        = string
  description = "Name for the cleanup lambda's IAM role"
}

variable "cleanup_lambda_method" {
  type        = string
  default     = "com.google.scp.operator.frontend.service.aws.AwsFailedJobQueueWriteCleanup"
  description = "Fully qualified method the cleanup lambda is called with"
}

variable "cleanup_lambda_memory_size" {
  type        = number
  default     = 2048
  description = "Amount of memory in MB to give the cleanup lambda"
}

variable "cleanup_lambda_runtime_timeout" {
  type        = number
  default     = 15
  description = <<-EOT
  Timeout for the cleanup lambda to run in seconds. This is the lambda runtime
  not the overall processing timeout.
  EOT
}

variable "metadata_db_stream_arn" {
  type        = string
  description = "The ARN of the DynamoDB stream the lambda will read from"
}

variable "metadata_db_arn" {
  type        = string
  description = "The ARN of the DynamoDB table the lambda will read/write from"
}

variable "jobqueue_sqs_url" {
  type        = string
  description = "The URL of the SQS Queue to use as the JobQueue"
}

variable "jobqueue_sqs_arn" {
  type        = string
  description = "The ARN of the SQS Queue to use as the JobQueue"
}

variable "frontend_cloudwatch_retention_days" {
  default     = "90"
  description = "The retention period for cloudwatch logs"
}

variable "metadata_db_table_name" {
  description = "The name of the metadata db table"
}

variable "max_window_age" {
  default     = "90"
  description = "The max reporting window used for aggregation requests"
}

variable "get_job_lambda_name" {
  default     = "get_job"
  description = "Name for lambda function"
}

variable "get_job_lambda_handler" {
  default     = "com.google.scp.operator.frontend.service.aws.GetJobApiGatewayHandler"
  description = "Full location to lambda handler"
}

variable "create_job_lambda_name" {
  default     = "create_job"
  description = "Name for lambda function"
}

variable "create_job_lambda_handler" {
  default     = "com.google.scp.operator.frontend.service.aws.CreateJobApiGatewayHandler"
  description = "Full location to lambda handler"
}

variable "frontend_lambda_local_jar" {
  type        = string
  description = "Path to the jar for the lambda"
}

variable "frontend_lambda_role_name" {
  type        = string
  description = "The name for the lambda's IAM role"
}

variable "create_job_lambda_timeout" {
  default     = 10
  description = "The timeout for the create job lambda"
}

variable "get_job_lambda_timeout" {
  default     = 10
  description = "The timeout for the create job lambda"
}

variable "create_job_lambda_size" {
  default     = 1024
  description = "The size in MB of the create job lambda"
}

variable "get_job_lambda_size" {
  default     = 1024
  description = "The size in MB of the create job lambda"
}

variable "get_job_lambda_provisioned_concurrency_enabled" {
  default     = false
  type        = bool
  description = "Whether to use use provisioned concurrency for get job lambda"
}

variable "create_job_lambda_provisioned_concurrency_enabled" {
  default     = false
  type        = bool
  description = "Whether to use use provisioned concurrency for create job lambda"
}

variable "get_job_lambda_provisioned_concurrency_count" {
  default     = 2
  description = "Number of lambda instances to initialize for get job lambda provisioned concurrency"
}

variable "create_job_lambda_provisioned_concurrency_count" {
  default     = 2
  description = "Number of lambda instances to initialize for create job lambda provisioned concurrency"
}

variable "job_metadata_ttl" {
  default     = 365
  description = "The ttl for job metadata records in days"
}

################################################################################
# API Gateway Variables
################################################################################

variable "api_env_stage_name" {
  default = "stage"
}

variable "api_version" {
  description = "Version of the created APIs. Eg: v1"
  type        = string
}

variable "api_description" {
  description = "Description of the API"
  type        = string
}
################################################################################
# Alarm Variables
################################################################################

variable "frontend_alarms_enabled" {
  description = "Enable alarms for frontend service"
  type        = bool
  default     = true
}

variable "frontend_sns_topic_arn" {
  description = "SNS topic ARN for alarm actions"
  type        = string
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

variable "region" {
  description = "AWS region to deploy components"
  type        = string
}
