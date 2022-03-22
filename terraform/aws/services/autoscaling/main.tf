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
  heartbeat_timeout      = 3600 // 1 hour
  lifecycle_transition   = "autoscaling:EC2_INSTANCE_TERMINATING"
}

module "worker_scale_in_hook_parameter" {
  source          = "../parameters"
  environment     = var.environment
  parameter_name  = "scale_in_hook"
  parameter_value = aws_autoscaling_lifecycle_hook.worker_scale_in_hook.name
}