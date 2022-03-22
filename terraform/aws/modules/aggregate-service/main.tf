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

provider "aws" {
  region = var.region
}

data "aws_caller_identity" "current" {}

module "metadata_db" {
  source      = "../../services/metadatadb"
  environment = var.environment
  table_name  = "${var.environment}-JobMetadata"

  # Cost ~$2.91 per month
  read_capacity  = 5
  write_capacity = 5
}

module "job_queue" {
  source      = "../../services/jobqueue"
  environment = var.environment
  region      = var.region
  queue_name  = "${var.environment}-JobQueue"

  #Alarms
  worker_alarms_enabled               = var.worker_alarms_enabled
  operator_sns_topic_arn              = module.operator_alarm_sns_topic.operator_alarm_sns_topic_arn
  job_queue_old_message_threshold_sec = var.job_queue_old_message_threshold_sec
  job_queue_alarm_eval_period_sec     = var.job_queue_alarm_eval_period_sec
}

module "max_job_num_attempts" {
  source          = "../../services/parameters"
  environment     = var.environment
  parameter_name  = "max_job_num_attempts"
  parameter_value = var.max_job_num_attempts_parameter
}

module "max_job_processing_time_parameter" {
  source          = "../../services/parameters"
  environment     = var.environment
  parameter_name  = "max_job_processing_time_seconds"
  parameter_value = var.max_job_processing_time_parameter
}

module "assume_role_parameter" {
  source          = "../../services/parameters"
  environment     = var.environment
  parameter_name  = "assume_role_arn"
  parameter_value = var.assume_role_parameter
}

module "kms_key_parameter" {
  source          = "../../services/parameters"
  environment     = var.environment
  parameter_name  = "kms_key_arn"
  parameter_value = var.kms_key_parameter
}

module "frontend" {
  source                               = "../../services/frontend"
  environment                          = var.environment
  frontend_api_name                    = "${var.environment}-${var.region}-frontend-api"
  lambda_package_storage_bucket_prefix = "${var.environment}-frontend-"

  change_handler_lambda_local_jar = var.change_handler_lambda
  change_handler_lambda_name      = "${var.environment}-ChangeHandler"
  change_handler_lambda_role_name = "${var.environment}-ChangeHandlerRole"
  change_handler_dlq_sqs_name     = "${var.environment}-ChangeHandlerDeadLetter"

  frontend_lambda_local_jar = var.frontend_lambda
  frontend_lambda_role_name = "${var.environment}-FrontendRole"
  get_job_lambda_name       = "${var.environment}-get-job"
  create_job_lambda_name    = "${var.environment}-create-job"

  metadata_db_stream_arn = module.metadata_db.metadata_db_stream_arn
  metadata_db_arn        = module.metadata_db.metadata_db_arn
  metadata_db_table_name = module.metadata_db.metadata_db_table_name

  jobqueue_sqs_url = module.job_queue.jobqueue_sqs_url
  jobqueue_sqs_arn = module.job_queue.jobqueue_sqs_arn

  cleanup_lambda_local_jar = var.sqs_write_failure_cleanup_lambda
  cleanup_lambda_name      = "${var.environment}-DeadLetterCleanup"
  cleanup_lambda_role_name = "${var.environment}-DeadLetterCleanupRole"

  region          = var.region
  api_version     = "v1alpha"
  api_description = "Aggregate Service Frontend API"

  #Alarms
  frontend_alarms_enabled                 = var.frontend_alarms_enabled
  frontend_sns_topic_arn                  = module.operator_alarm_sns_topic.operator_alarm_sns_topic_arn
  frontend_alarm_eval_period_sec          = var.frontend_alarm_eval_period_sec
  frontend_lambda_error_threshold         = var.frontend_lambda_error_threshold
  frontend_lambda_max_throttles_threshold = var.frontend_lambda_max_throttles_threshold
  frontend_lambda_max_duration_threshold  = var.frontend_lambda_max_duration_threshold
  frontend_api_max_latency_ms             = var.frontend_api_max_latency_ms
  frontend_5xx_threshold                  = var.frontend_5xx_threshold
  frontend_4xx_threshold                  = var.frontend_4xx_threshold
  change_handler_dlq_threshold            = var.change_handler_dlq_threshold
  change_handler_max_latency_ms           = var.change_handler_max_latency_ms
}

module "worker_service" {
  source = "../../services/worker"

  region            = var.region
  instance_type     = var.instance_type
  key_name          = var.key_name
  ec2_iam_role_name = "${var.environment}-AggregationServiceWorkerRole"

  worker_template_name = "${var.environment}-aggregation-service-template"

  ami_name   = var.ami_name
  ami_owners = var.ami_owners

  environment       = var.environment
  ec2_instance_name = "${var.environment}-aggregation-service"

  metadata_db_table_arn       = module.metadata_db.metadata_db_arn
  job_queue_arn               = module.job_queue.jobqueue_sqs_arn
  coordinator_assume_role_arn = var.assume_role_parameter

  #Alarms
  worker_alarms_enabled            = var.worker_alarms_enabled
  operator_sns_topic_arn           = module.operator_alarm_sns_topic.operator_alarm_sns_topic_arn
  job_client_error_threshold       = var.job_client_error_threshold
  job_validation_failure_threshold = var.job_validation_failure_threshold
  worker_job_error_threshold       = var.worker_job_error_threshold
  worker_alarm_eval_period_sec     = var.worker_alarm_eval_period_sec
  worker_alarm_metric_dimensions   = var.worker_alarm_metric_dimensions
}

module "operator_alarm_sns_topic" {
  source = "../../services/alarmsnstopic"

  environment              = var.environment
  alarm_notification_email = var.alarm_notification_email
  region                   = var.region
}

module "worker_autoscaling" {
  source = "../../services/autoscaling"

  environment             = var.environment
  region                  = var.region
  asg_name                = "${var.environment}-aggregation-service-workers"
  worker_template_id      = module.worker_service.worker_template_id
  worker_template_version = module.worker_service.worker_template_version
  worker_subnet_ids       = module.worker_service.worker_subnet_ids

  enable_autoscaling             = true
  initial_capacity_ec2_instances = var.initial_capacity_ec2_instances
  min_ec2_instances              = var.min_capacity_ec2_instances
  max_ec2_instances              = var.max_capacity_ec2_instances

  jobqueue_sqs_url = module.job_queue.jobqueue_sqs_url
  jobqueue_sqs_arn = module.job_queue.jobqueue_sqs_arn

  lambda_package_storage_bucket_prefix = "${var.environment}-worker-"

  asg_capacity_handler_lambda_local_jar = var.asg_capacity_handler_lambda
  asg_capacity_handler_lambda_name      = "${var.environment}-AsgCapacityHandler"
  asg_capacity_lambda_role_name         = "${var.environment}-AsgCapacityLambdaRole"

  terminated_instance_handler_lambda_local_jar = var.terminated_instance_handler_lambda
  terminated_instance_handler_lambda_name      = "${var.environment}-TerminatedInstanceHandler"
  terminated_instance_lambda_role_name         = "${var.environment}-TerminatedInstanceLambdaRole"

  #Alarms
  worker_alarms_enabled                         = var.worker_alarms_enabled
  operator_sns_topic_arn                        = module.operator_alarm_sns_topic.operator_alarm_sns_topic_arn
  asg_capacity_lambda_error_threshold           = var.asg_capacity_lambda_error_threshold
  asg_capacity_lambda_duration_threshold        = var.asg_capacity_lambda_duration_threshold
  asg_capacity_alarm_eval_period_sec            = var.asg_capacity_alarm_eval_period_sec
  terminated_instance_lambda_error_threshold    = var.terminated_instance_lambda_error_threshold
  terminated_instance_lambda_duration_threshold = var.terminated_instance_lambda_duration_threshold
  terminated_instance_alarm_eval_period_sec     = var.terminated_instance_alarm_eval_period_sec
  asg_max_instances_alarm_ratio                 = var.asg_max_instances_alarm_ratio
  autoscaling_alarm_eval_period_sec             = var.autoscaling_alarm_eval_period_sec
}

module "frontend_dashboard" {
  source = "../../services/frontenddashboard"

  environment                   = var.environment
  region                        = var.region
  frontend_dashboard_gateway_id = module.frontend.frontend_api_id
}

module "worker_dashboard" {
  source = "../../services/workerdashboard"

  environment = var.environment
  region      = var.region
}