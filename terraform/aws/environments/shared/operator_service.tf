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

locals {
  coordinator_a_assume_role_parameter = var.coordinator_a_assume_role_parameter != "" ? var.coordinator_a_assume_role_parameter : var.assume_role_parameter
  # TODO: find a better default behavior, defaulting to coordinator A allows the
  # code to work but this behavior is incorrect and authentication will fail if
  # this default value is used.
  coordinator_b_assume_role_parameter = var.coordinator_b_assume_role_parameter != "" ? var.coordinator_b_assume_role_parameter : local.coordinator_a_assume_role_parameter
}

module "operator_service" {
  source = "../../applications/operator-service"

  region      = var.region
  environment = var.environment

  instance_type = var.instance_type
  ami_name      = var.ami_name
  ami_owners    = var.ami_owners

  enclave_cpu_count  = var.enclave_cpu_count
  enclave_memory_mib = var.enclave_memory_mib

  kms_key_parameter                   = var.kms_key_parameter
  coordinator_a_assume_role_parameter = local.coordinator_a_assume_role_parameter
  coordinator_b_assume_role_parameter = local.coordinator_b_assume_role_parameter
  worker_ssh_public_key               = var.worker_ssh_public_key

  max_job_num_attempts_parameter    = var.max_job_num_attempts_parameter
  max_job_processing_time_parameter = var.max_job_processing_time_parameter

  initial_capacity_ec2_instances = var.initial_capacity_ec2_instances
  min_capacity_ec2_instances     = var.min_capacity_ec2_instances
  max_capacity_ec2_instances     = var.max_capacity_ec2_instances

  termination_hook_heartbeat_timeout_sec     = var.termination_hook_heartbeat_timeout_sec
  termination_hook_timeout_extension_enabled = var.termination_hook_timeout_extension_enabled
  termination_hook_heartbeat_frequency_sec   = var.termination_hook_heartbeat_frequency_sec
  termination_hook_max_timeout_extension_sec = var.termination_hook_max_timeout_extension_sec

  change_handler_lambda              = var.change_handler_lambda
  frontend_lambda                    = var.frontend_lambda
  sqs_write_failure_cleanup_lambda   = var.sqs_write_failure_cleanup_lambda
  asg_capacity_handler_lambda        = var.asg_capacity_handler_lambda
  terminated_instance_handler_lambda = var.terminated_instance_handler_lambda

  alarm_notification_email = var.alarm_notification_email

  # MetadataDB
  metadatadb_read_capacity  = var.metadatadb_read_capacity
  metadatadb_write_capacity = var.metadatadb_write_capacity

  # AsgInstancesDB
  asginstances_db_ttl_days = var.asginstances_db_ttl_days

  # Frontend Alarms
  frontend_alarms_enabled                 = var.frontend_alarms_enabled
  frontend_alarm_eval_period_sec          = var.frontend_alarm_eval_period_sec
  frontend_lambda_error_threshold         = var.frontend_lambda_error_threshold
  frontend_lambda_max_throttles_threshold = var.frontend_lambda_max_throttles_threshold
  frontend_lambda_max_duration_threshold  = var.frontend_lambda_max_duration_threshold
  frontend_api_max_latency_ms             = var.frontend_api_max_latency_ms
  frontend_5xx_threshold                  = var.frontend_5xx_threshold
  frontend_4xx_threshold                  = var.frontend_4xx_threshold
  change_handler_dlq_threshold            = var.change_handler_dlq_threshold
  change_handler_max_latency_ms           = var.change_handler_max_latency_ms

  # Worker Alarms
  worker_alarms_enabled            = var.worker_alarms_enabled
  job_client_error_threshold       = var.job_client_error_threshold
  job_validation_failure_threshold = var.job_validation_failure_threshold
  worker_job_error_threshold       = var.worker_job_error_threshold
  worker_alarm_eval_period_sec     = var.worker_alarm_eval_period_sec
  worker_alarm_metric_dimensions   = var.worker_alarm_metric_dimensions

  # Autoscaling Alarms
  asg_capacity_lambda_error_threshold           = var.asg_capacity_lambda_error_threshold
  asg_capacity_lambda_duration_threshold        = var.asg_capacity_lambda_duration_threshold
  asg_capacity_alarm_eval_period_sec            = var.asg_capacity_alarm_eval_period_sec
  terminated_instance_lambda_error_threshold    = var.terminated_instance_lambda_error_threshold
  terminated_instance_lambda_duration_threshold = var.terminated_instance_lambda_duration_threshold
  terminated_instance_alarm_eval_period_sec     = var.terminated_instance_alarm_eval_period_sec
  asg_max_instances_alarm_ratio                 = var.asg_max_instances_alarm_ratio
  autoscaling_alarm_eval_period_sec             = var.autoscaling_alarm_eval_period_sec

  # Job Queue Alarms
  job_queue_old_message_threshold_sec = var.job_queue_old_message_threshold_sec
  job_queue_alarm_eval_period_sec     = var.job_queue_alarm_eval_period_sec

  # MetadataDB Alarms
  metadatadb_read_capacity_usage_ratio_alarm_threshold  = var.metadatadb_read_capacity_usage_ratio_alarm_threshold
  metadatadb_write_capacity_usage_ratio_alarm_threshold = var.metadatadb_write_capacity_usage_ratio_alarm_threshold
  metadatadb_alarm_eval_period_sec                      = var.metadatadb_alarm_eval_period_sec

  # VPC
  enable_user_provided_vpc             = var.enable_user_provided_vpc
  user_provided_vpc_security_group_ids = var.user_provided_vpc_security_group_ids
  user_provided_vpc_subnet_ids         = var.user_provided_vpc_subnet_ids
  vpc_cidr                             = var.vpc_cidr
  vpc_availability_zones               = var.vpc_availability_zones

  # Notifications
  enable_job_completion_notifications = var.enable_job_completion_notifications

  # OpenTelemetry
  allowed_otel_metrics = var.allowed_otel_metrics
  min_log_level        = var.min_log_level

  # Per-Coordinator Configs
  coordinator_configs             = var.coordinator_configs
  coordinator_wif_config_override = var.coordinator_wif_config_override
}

# Used by Terraform to treat any existing resources belonging to formerly known
# `aggregate_service` as if they had been created for the newly named `operator_service`
moved {
  from = module.aggregate_service
  to   = module.operator_service
}

output "frontend_api_id" {
  value = module.operator_service.frontend_api_id
}

output "create_job_endpoint" {
  value = module.operator_service.create_job_endpoint
}

output "get_job_endpoint" {
  value = module.operator_service.get_job_endpoint
}

output "notifications_sns_topic_arn" {
  value       = module.operator_service.notifications_sns_topic_arn
  description = "The ARN of the SNS notifications topic."
}

output "worker_role_name" {
  value       = module.operator_service.worker_role_name
  description = "Name of the role used by the worker service."
}
