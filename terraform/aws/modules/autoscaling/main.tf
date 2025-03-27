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

terraform {
  required_version = "~> 1.2.3"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.0"
    }
  }
}

################################################################################
# Autoscaling
################################################################################

resource "aws_autoscaling_group" "worker_group" {
  name                = var.asg_name
  vpc_zone_identifier = var.worker_subnet_ids
  max_size            = var.enable_autoscaling ? var.max_ec2_instances : var.initial_capacity_ec2_instances
  min_size            = var.enable_autoscaling ? var.min_ec2_instances : var.initial_capacity_ec2_instances

  launch_template {
    id      = var.worker_template_id
    version = var.worker_template_version
  }

  lifecycle {
    create_before_destroy = true
  }

  instance_refresh {
    strategy = "Rolling"
    preferences {
      instance_warmup        = 60
      min_healthy_percentage = 80
    }
  }

  enabled_metrics = [
    "GroupDesiredCapacity",
    "GroupInServiceInstances",
    "GroupPendingInstances",
    "GroupTerminatingInstances",
    "GroupTotalInstances"
  ]
}

resource "aws_autoscaling_lifecycle_hook" "worker_scale_in_hook" {
  name                   = "worker-scale-in-hook-${var.environment}"
  autoscaling_group_name = aws_autoscaling_group.worker_group.name
  default_result         = "CONTINUE"
  heartbeat_timeout      = var.termination_hook_heartbeat_timeout_sec
  lifecycle_transition   = "autoscaling:EC2_INSTANCE_TERMINATING"
}

module "worker_scale_in_hook_parameter" {
  source                = "../parameters"
  environment           = var.environment
  parameter_name        = "SCALE_IN_HOOK"
  legacy_parameter_name = "scale_in_hook"
  parameter_value       = aws_autoscaling_lifecycle_hook.worker_scale_in_hook.name
}

module "worker_autoscaling_group_parameter" {
  source          = "../parameters"
  environment     = var.environment
  parameter_name  = "WORKER_AUTOSCALING_GROUP"
  parameter_value = aws_autoscaling_group.worker_group.name
}

module "lifecycle_action_heartbeat_timeout_parameter" {
  source          = "../parameters"
  environment     = var.environment
  parameter_name  = "LIFECYCLE_ACTION_HEARTBEAT_TIMEOUT"
  parameter_value = var.termination_hook_heartbeat_timeout_sec
}

module "lifecycle_action_heartbeat_enabled_parameter" {
  source          = "../parameters"
  environment     = var.environment
  parameter_name  = "LIFECYCLE_ACTION_HEARTBEAT_ENABLED"
  parameter_value = var.termination_hook_timeout_extension_enabled
}

module "lifecycle_action_heartbeat_frequency_parameter" {
  source          = "../parameters"
  environment     = var.environment
  parameter_name  = "LIFECYCLE_ACTION_HEARTBEAT_FREQUENCY"
  parameter_value = var.termination_hook_heartbeat_frequency_sec
}

module "max_lifecycle_action_heartbeat_timeout_extension_parameter" {
  source          = "../parameters"
  environment     = var.environment
  parameter_name  = "MAX_LIFECYCLE_ACTION_TIMEOUT_EXTENSION"
  parameter_value = var.termination_hook_max_timeout_extension_sec
}
