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

locals {
  operator_alarm_sns_topic_enabled = var.frontend_alarms_enabled || var.worker_alarms_enabled
  operator_aws_account_id          = data.aws_caller_identity.current.account_id

  # Generate wif configs based on coordinator config, otherwise use the overrides.
  override_wif = length(var.coordinator_wif_config_override) != 0
  coordinator_wif_configs = {
    for name, config in var.coordinator_configs : name =>
    {
      gcp_project_number                 = config.gcp_project_number,
      workload_identity_pool_id          = local.override_wif ? var.coordinator_wif_config_override[name].workload_identity_pool_id : "${config.env_name}-aws-wip-${local.operator_aws_account_id}",
      workload_identity_pool_provider_id = local.override_wif ? var.coordinator_wif_config_override[name].workload_identity_pool_provider_id : "wipp-${local.operator_aws_account_id}",
      sa_email                           = local.override_wif ? var.coordinator_wif_config_override[name].sa_email : "${config.env_name}-fed-aws-${local.operator_aws_account_id}@${config.gcp_project_id}.iam.gserviceaccount.com"
    }
  }
  coordinator_assume_roles = {
    for name, config in var.coordinator_configs : name =>
    "arn:aws:iam::${config.aws_account_id}:role/${config.env_name}-assumed-role-${local.operator_aws_account_id}"
  }
  # Fallback to legacy roles provided via parameters if coordinator_configs were not specified.
  use_legacy_roles          = length(var.coordinator_configs) == 0
  coordinator_a_assume_role = local.use_legacy_roles ? var.coordinator_a_assume_role_parameter : local.coordinator_assume_roles["COORDINATOR_A"]
  coordinator_b_assume_role = local.use_legacy_roles ? var.coordinator_b_assume_role_parameter : local.coordinator_assume_roles["COORDINATOR_B"]
}

module "bazel" {
  source = "../../modules/bazel"
}

module "metadata_db" {
  source      = "../../modules/metadatadb"
  environment = var.environment
  table_name  = "${var.environment}-JobMetadata"

  read_capacity  = var.metadatadb_read_capacity
  write_capacity = var.metadatadb_write_capacity

  #Alarms
  worker_alarms_enabled = var.worker_alarms_enabled

  eval_period_sec = var.metadatadb_alarm_eval_period_sec
  sns_topic_arn   = local.operator_alarm_sns_topic_enabled ? module.operator_alarm_sns_topic[0].operator_alarm_sns_topic_arn : ""

  metadatadb_read_capacity_usage_ratio_alarm_threshold  = var.metadatadb_read_capacity_usage_ratio_alarm_threshold
  metadatadb_write_capacity_usage_ratio_alarm_threshold = var.metadatadb_write_capacity_usage_ratio_alarm_threshold
}

module "asginstances_db" {
  source      = "../../modules/asginstancesdb"
  environment = var.environment
  table_name  = "${var.environment}-AsgInstances"
}

module "job_queue" {
  source      = "../../modules/jobqueue"
  environment = var.environment
  region      = var.region
  queue_name  = "${var.environment}-JobQueue"

  #Alarms
  worker_alarms_enabled               = var.worker_alarms_enabled
  operator_sns_topic_arn              = local.operator_alarm_sns_topic_enabled ? module.operator_alarm_sns_topic[0].operator_alarm_sns_topic_arn : ""
  job_queue_old_message_threshold_sec = var.job_queue_old_message_threshold_sec
  job_queue_alarm_eval_period_sec     = var.job_queue_alarm_eval_period_sec
}

module "max_job_num_attempts" {
  source                = "../../modules/parameters"
  environment           = var.environment
  parameter_name        = "MAX_JOB_NUM_ATTEMPTS"
  legacy_parameter_name = "max_job_num_attempts"
  parameter_value       = var.max_job_num_attempts_parameter
}

module "max_job_processing_time_parameter" {
  source                = "../../modules/parameters"
  environment           = var.environment
  parameter_name        = "MAX_JOB_PROCESSING_TIME_SECONDS"
  legacy_parameter_name = "max_job_processing_time_seconds"
  parameter_value       = var.max_job_processing_time_parameter
}

module "assume_role_parameter" {
  source                = "../../modules/parameters"
  environment           = var.environment
  parameter_name        = "COORDINATOR_ROLE"
  legacy_parameter_name = "assume_role_arn"
  parameter_value       = local.coordinator_a_assume_role
}

module "coordinator_a_assume_role_parameter" {
  source                = "../../modules/parameters"
  environment           = var.environment
  parameter_name        = "COORDINATOR_A_ROLE"
  legacy_parameter_name = "coordinator_a_assume_role_arn"
  parameter_value       = local.coordinator_a_assume_role
}

module "coordinator_b_assume_role_parameter" {
  source                = "../../modules/parameters"
  environment           = var.environment
  parameter_name        = "COORDINATOR_B_ROLE"
  legacy_parameter_name = "coordinator_b_assume_role_arn"
  parameter_value       = local.coordinator_b_assume_role
}

module "kms_key_parameter" {
  source                = "../../modules/parameters"
  environment           = var.environment
  parameter_name        = "COORDINATOR_KMS_ARN"
  legacy_parameter_name = "kms_key_arn"
  parameter_value       = var.kms_key_parameter
}

module "notifications_topic_arn" {
  count           = var.enable_job_completion_notifications ? 1 : 0
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "NOTIFICATIONS_TOPIC_ID"
  parameter_value = module.notifications[0].notifications_sns_topic_arn
}

module "frontend" {
  source                               = "../../modules/frontend"
  environment                          = var.environment
  frontend_api_name                    = "${var.environment}-${var.region}-frontend-api"
  lambda_package_storage_bucket_prefix = "${var.environment}-frontend-"

  change_handler_lambda_local_jar = var.change_handler_lambda == "" ? "${module.bazel.bazel_bin}/java/com/google/scp/operator/frontend/service/aws/AwsChangeHandlerLambda_deploy.jar" : var.change_handler_lambda
  change_handler_lambda_name      = "${var.environment}-ChangeHandler"
  change_handler_lambda_role_name = "${var.environment}-ChangeHandlerRole"
  change_handler_dlq_sqs_name     = "${var.environment}-ChangeHandlerDeadLetter"

  frontend_lambda_local_jar = var.frontend_lambda == "" ? "${module.bazel.bazel_bin}/java/com/google/scp/operator/frontend/AwsApigatewayFrontend_deploy.jar" : var.frontend_lambda
  frontend_lambda_role_name = "${var.environment}-FrontendRole"
  get_job_lambda_name       = "${var.environment}-get-job"
  create_job_lambda_name    = "${var.environment}-create-job"

  metadata_db_stream_arn = module.metadata_db.metadata_db_stream_arn
  metadata_db_arn        = module.metadata_db.metadata_db_arn
  metadata_db_table_name = module.metadata_db.metadata_db_table_name

  jobqueue_sqs_url = module.job_queue.jobqueue_sqs_url
  jobqueue_sqs_arn = module.job_queue.jobqueue_sqs_arn

  cleanup_lambda_local_jar = var.sqs_write_failure_cleanup_lambda == "" ? "${module.bazel.bazel_bin}/java/com/google/scp/operator/frontend/service/aws/AwsFrontendCleanupLambda_deploy.jar" : var.sqs_write_failure_cleanup_lambda
  cleanup_lambda_name      = "${var.environment}-DeadLetterCleanup"
  cleanup_lambda_role_name = "${var.environment}-DeadLetterCleanupRole"

  region          = var.region
  api_version     = "v1alpha"
  api_description = "Aggregate Service Frontend API"

  #Alarms
  frontend_alarms_enabled                 = var.frontend_alarms_enabled
  frontend_sns_topic_arn                  = local.operator_alarm_sns_topic_enabled ? module.operator_alarm_sns_topic[0].operator_alarm_sns_topic_arn : ""
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
  source = "../../modules/worker"

  region                = var.region
  instance_type         = var.instance_type
  worker_ssh_public_key = var.worker_ssh_public_key
  ec2_iam_role_name     = "${var.environment}-AggregationServiceWorkerRole"

  worker_template_name = "${var.environment}-aggregation-service-template"

  ami_name   = var.ami_name
  ami_owners = var.ami_owners

  enclave_cpu_count  = var.enclave_cpu_count
  enclave_memory_mib = var.enclave_memory_mib

  environment       = var.environment
  ec2_instance_name = "${var.environment}-aggregation-service"

  metadata_db_table_arn         = module.metadata_db.metadata_db_arn
  asg_instances_table_arn       = module.asginstances_db.asginstances_db_arn
  job_queue_arn                 = module.job_queue.jobqueue_sqs_arn
  coordinator_a_assume_role_arn = local.coordinator_a_assume_role
  coordinator_b_assume_role_arn = local.coordinator_b_assume_role

  #Alarms
  worker_alarms_enabled            = var.worker_alarms_enabled
  operator_sns_topic_arn           = local.operator_alarm_sns_topic_enabled ? module.operator_alarm_sns_topic[0].operator_alarm_sns_topic_arn : ""
  job_client_error_threshold       = var.job_client_error_threshold
  job_validation_failure_threshold = var.job_validation_failure_threshold
  worker_job_error_threshold       = var.worker_job_error_threshold
  worker_alarm_eval_period_sec     = var.worker_alarm_eval_period_sec
  worker_alarm_metric_dimensions   = var.worker_alarm_metric_dimensions
  worker_security_group_ids        = var.enable_user_provided_vpc ? var.user_provided_vpc_security_group_ids : [module.vpc[0].allow_internal_ingress_sg_id, module.vpc[0].allow_egress_sg_id]

  # VPC
  dynamodb_vpc_endpoint_id = module.vpc[0].dynamodb_vpc_endpoint_id
  s3_vpc_endpoint_id       = module.vpc[0].s3_vpc_endpoint_id

  # OpenTelemetry
  allowed_otel_metrics = var.allowed_otel_metrics
  min_log_level        = var.min_log_level
}

module "operator_alarm_sns_topic" {
  count  = local.operator_alarm_sns_topic_enabled ? 1 : 0
  source = "../../modules/alarmsnstopic"

  environment              = var.environment
  alarm_notification_email = var.alarm_notification_email
  region                   = var.region
}

module "worker_autoscaling" {
  source = "../../modules/autoscaling"

  environment             = var.environment
  region                  = var.region
  asg_name                = "${var.environment}-aggregation-service-workers"
  worker_template_id      = module.worker_service.worker_template_id
  worker_template_version = module.worker_service.worker_template_version
  worker_subnet_ids       = var.enable_user_provided_vpc ? var.user_provided_vpc_subnet_ids : module.vpc[0].private_subnet_ids

  enable_autoscaling             = true
  initial_capacity_ec2_instances = var.initial_capacity_ec2_instances
  min_ec2_instances              = var.min_capacity_ec2_instances
  max_ec2_instances              = var.max_capacity_ec2_instances

  termination_hook_heartbeat_timeout_sec     = var.termination_hook_heartbeat_timeout_sec
  termination_hook_timeout_extension_enabled = var.termination_hook_timeout_extension_enabled
  termination_hook_heartbeat_frequency_sec   = var.termination_hook_heartbeat_frequency_sec
  termination_hook_max_timeout_extension_sec = var.termination_hook_max_timeout_extension_sec

  jobqueue_sqs_url = module.job_queue.jobqueue_sqs_url
  jobqueue_sqs_arn = module.job_queue.jobqueue_sqs_arn

  lambda_package_storage_bucket_prefix = "${var.environment}-worker-"

  asg_capacity_handler_lambda_local_jar = var.asg_capacity_handler_lambda == "" ? "${module.bazel.bazel_bin}/java/com/google/scp/operator/autoscaling/app/aws/AsgCapacityHandlerLambda_deploy.jar" : var.asg_capacity_handler_lambda
  asg_capacity_handler_lambda_name      = "${var.environment}-AsgCapacityHandler"
  asg_capacity_lambda_role_name         = "${var.environment}-AsgCapacityLambdaRole"

  terminated_instance_handler_lambda_local_jar = var.terminated_instance_handler_lambda == "" ? "${module.bazel.bazel_bin}/java/com/google/scp/operator/autoscaling/app/aws/TerminatedInstanceHandlerLambda_deploy.jar" : var.terminated_instance_handler_lambda
  terminated_instance_handler_lambda_name      = "${var.environment}-TerminatedInstanceHandler"
  terminated_instance_lambda_role_name         = "${var.environment}-TerminatedInstanceLambdaRole"
  asginstances_db_table_name                   = module.asginstances_db.asginstances_db_table_name
  asginstances_db_arn                          = module.asginstances_db.asginstances_db_arn
  asginstances_db_ttl_days                     = var.asginstances_db_ttl_days

  #Alarms
  worker_alarms_enabled                         = var.worker_alarms_enabled
  operator_sns_topic_arn                        = local.operator_alarm_sns_topic_enabled ? module.operator_alarm_sns_topic[0].operator_alarm_sns_topic_arn : ""
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
  source = "../../modules/frontenddashboard"

  environment                   = var.environment
  region                        = var.region
  frontend_dashboard_gateway_id = module.frontend.frontend_api_id
}

module "worker_dashboard" {
  source = "../../modules/workerdashboard"

  environment = var.environment
  region      = var.region
}

module "vpc" {
  count  = var.enable_user_provided_vpc ? 0 : 1
  source = "../../modules/vpc"

  environment            = var.environment
  vpc_cidr               = var.vpc_cidr
  vpc_availability_zones = var.vpc_availability_zones

  lambda_execution_role_ids = []

  dynamodb_allowed_principal_arns = [module.worker_service.worker_enclave_role_arn]
  dynamodb_arns                   = [module.metadata_db.metadata_db_arn, module.asginstances_db.asginstances_db_arn]

  s3_allowed_principal_arns = [module.worker_service.worker_enclave_role_arn]
}

module "notifications" {
  count  = var.enable_job_completion_notifications ? 1 : 0
  source = "../../modules/notifications"

  environment = var.environment
}

module "coordinator_wif_configuration" {
  for_each = local.coordinator_wif_configs
  source   = "../../modules/wifconfiguration"

  environment                        = var.environment
  gcp_project_number                 = each.value.gcp_project_number
  workload_identity_pool_id          = each.value.workload_identity_pool_id
  workload_identity_pool_provider_id = each.value.workload_identity_pool_provider_id
  sa_email                           = each.value.sa_email
  coordinator_name                   = each.key
}
