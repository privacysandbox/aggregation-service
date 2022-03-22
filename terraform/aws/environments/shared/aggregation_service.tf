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

data "local_file" "version" {
  filename = "../../../../VERSION"
}
locals {
  service_version = data.local_file.version.content
}

module "aggregate_service" {
  source = "../../modules/aggregate-service"

  region      = var.region
  environment = var.environment

  instance_type = var.instance_type
  ami_name      = var.ami_name
  ami_owners    = var.ami_owners

  assume_role_parameter = var.assume_role_parameter
  kms_key_parameter     = var.kms_key_parameter

  max_job_num_attempts_parameter    = var.max_job_num_attempts_parameter
  max_job_processing_time_parameter = var.max_job_processing_time_parameter

  initial_capacity_ec2_instances = var.initial_capacity_ec2_instances
  min_capacity_ec2_instances     = var.min_capacity_ec2_instances
  max_capacity_ec2_instances     = var.max_capacity_ec2_instances

  # Note: these relatives paths only work when contained within the exported tar layout.
  change_handler_lambda              = "../../jars/AwsChangeHandlerLambda_${local.service_version}.jar"
  frontend_lambda                    = "../../jars/aws_apigateway_frontend_${local.service_version}.jar"
  sqs_write_failure_cleanup_lambda   = "../../jars/AwsFrontendCleanupLambda_${local.service_version}.jar"
  asg_capacity_handler_lambda        = "../../jars/AsgCapacityHandlerLambda_${local.service_version}.jar"
  terminated_instance_handler_lambda = "../../jars/TerminatedInstanceHandlerLambda_${local.service_version}.jar"

  alarm_notification_email = var.alarm_notification_email

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
}

output "frontend_api_id" {
  value = module.aggregate_service.frontend_api_id
}

output "frontend_api_endpoint" {
  value = module.aggregate_service.frontend_api_endpoint
}

output "create_job_endpoint" {
  value = module.aggregate_service.create_job_endpoint
}

output "get_job_endpoint" {
  value = module.aggregate_service.get_job_endpoint
}