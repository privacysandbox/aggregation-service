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

locals {
  // how often to collect the metric in seconds
  metric_period = "60"
}

locals {
  metrics_map = {
    cpu_usage = "process.runtime.jvm.CPU.utilization"
    memory    = "process.runtime.jvm.memory.utilization_ratio"
  }
  // Replace the metric name to match the real name in OTel
  all_otel_metrics = [
    for metric in var.allowed_otel_metrics : try(local.metrics_map[metric], metric)
  ]
  base_otel_metrics = [
    for metric in local.all_otel_metrics : metric if contains(values(local.metrics_map), metric)
  ]
  // Add job success and fail metrics
  otel_metrics = contains(var.allowed_otel_metrics, "job_success_metrics") ? concat(local.base_otel_metrics, [
    "job_success_counter", "job_fail_counter"
  ]) : local.base_otel_metrics

  // Excluding job success metrics from being collected as part of spans/traces.
  // Exporting as metrics is sufficient to track using cloud monitoring,
  // hence we don't export as trace similar to cpu and memory metrics.
  modified_otel_metrics = setsubtract(var.allowed_otel_metrics, ["job_success_metrics"])
  otel_spans = [
    for span in local.modified_otel_metrics : span if !contains(values(local.metrics_map), span)
  ]
  // No logs will be exported if set to "". Setting it to the highest severity level to filter out all the logs.
  min_log_level = var.min_log_level == "" ? "FATAL4" : var.min_log_level
}

################################################################################
# EC2 Instances
################################################################################

resource "aws_key_pair" "authorized_key" {
  count      = var.worker_ssh_public_key != "" ? 1 : 0
  key_name   = "${var.environment}_ssh_public_key"
  public_key = var.worker_ssh_public_key
}

data "aws_ami" "worker_image" {
  most_recent = true
  owners      = var.ami_owners
  filter {
    name = "name"
    values = [
      // AMI name format {ami-name}--YYYY-MM-DD'T'hh-mm-ssZ
      "${var.ami_name}--*"
    ]
  }
  filter {
    name = "state"
    values = [
      "available"
    ]
  }
}

resource "aws_iam_instance_profile" "worker_profile" {
  name = "${var.ec2_iam_role_name}Profile"
  role = var.ec2_iam_role_name

  depends_on = [aws_iam_role.enclave_role]
}

resource "aws_launch_template" "worker_template" {
  name = var.worker_template_name

  // TODO: Explore this parameter
  //  cpu_options {
  //    core_count       = 4
  //    threads_per_core = 1
  //  }

  credit_specification {
    cpu_credits = "standard"
  }

  iam_instance_profile {
    name = aws_iam_instance_profile.worker_profile.name
  }

  image_id = data.aws_ami.worker_image.id

  instance_type = var.instance_type

  key_name = var.worker_ssh_public_key != "" ? aws_key_pair.authorized_key[0].key_name : null

  monitoring {
    enabled = true
  }

  enclave_options {
    enabled = true
  }

  tag_specifications {
    resource_type = "instance"
    tags = {
      Name               = "${var.service}-${var.environment}"
      service            = var.service
      environment        = var.environment
      enclave_cpu_count  = var.enclave_cpu_count
      enclave_memory_mib = var.enclave_memory_mib
      otel_metrics       = jsonencode(local.otel_metrics)
      otel_spans         = jsonencode(local.otel_spans)
      min_log_level      = local.min_log_level
    }
  }

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  vpc_security_group_ids = var.worker_security_group_ids
}

################################################################################
# IAM Role
################################################################################

# This policy is a pre-requisite to enable SSM connection to the instance
data "aws_iam_policy" "SSMManagedInstanceCore" {
  arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role" "enclave_role" {
  name = var.ec2_iam_role_name

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowEc2AssumeRole"
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      },
    ]
  })

  managed_policy_arns = [data.aws_iam_policy.SSMManagedInstanceCore.arn]
}

data "aws_iam_policy_document" "enclave_policy_doc" {
  statement {
    sid       = "AllowAssumeRole"
    effect    = "Allow"
    actions   = ["sts:AssumeRole"]
    resources = [var.coordinator_a_assume_role_arn]
  }

  statement {
    sid    = "AllowSelectActionsOnSqsQueue"
    effect = "Allow"
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:SendMessage",
      "sqs:ChangeMessageVisibility",
    ]
    resources = [var.job_queue_arn]
  }

  statement {
    sid    = "AllowCoreEc2AutoscalingActions"
    effect = "Allow"
    actions = [
      "ec2:DescribeTags",
      "autoscaling:CompleteLifecycleAction",
      "autoscaling:DescribeAutoScalingInstances",
      "autoscaling:SetInstanceHealth",
      "autoscaling:RecordLifecycleActionHeartbeat",
    ]
    resources = ["*"]
  }

  statement {
    sid       = "AllowSsmGetParameters"
    effect    = "Allow"
    actions   = ["ssm:GetParameters"]
    resources = ["arn:aws:ssm:*:*:parameter/*"]
  }

  statement {
    sid       = "AllowCloudWatchPutMetricData"
    effect    = "Allow"
    actions   = ["cloudwatch:PutMetricData"]
    resources = ["*"]
  }

  statement {
    sid    = "AllowDdbAccessFromSpecificVPCe"
    effect = "Allow"
    actions = [
      "dynamodb:ConditionCheckItem",
      "dynamodb:GetItem",
      "dynamodb:PutItem",
    ]
    resources = [var.metadata_db_table_arn, var.asg_instances_table_arn]
    condition {
      test     = "StringEquals"
      values   = [var.dynamodb_vpc_endpoint_id]
      variable = "aws:sourceVpce"
    }
  }

  statement {
    sid       = "DenyDdbAccessFromAnyOtherEndpoints"
    effect    = "Deny"
    actions   = ["dynamodb:*"]
    resources = [var.metadata_db_table_arn, var.asg_instances_table_arn]
    condition {
      test     = "StringNotEquals"
      values   = [var.dynamodb_vpc_endpoint_id]
      variable = "aws:sourceVpce"
    }
  }

  statement {
    sid    = "AllowS3AccessFromSpecificVPCe"
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:ListBucket",
    ]
    resources = ["*"]
    condition {
      test     = "StringEquals"
      values   = [var.s3_vpc_endpoint_id]
      variable = "aws:sourceVpce"
    }
  }

  statement {
    sid       = "DenyS3AccessFromAnyOtherEndpoints"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = ["*"]
    condition {
      test     = "StringNotEquals"
      values   = [var.s3_vpc_endpoint_id]
      variable = "aws:sourceVpce"
    }
  }

  statement {
    sid    = "AllowOTelAccess"
    effect = "Allow"
    actions = [
      "logs:PutLogEvents",
      "logs:CreateLogStream",
      "logs:CreateLogGroup",
      "xray:PutTelemetryRecords",
      "xray:PutTraceSegments",
    ]
    resources = ["*"]
  }

  statement {
    sid       = "AllowSNSPublishing"
    effect    = "Allow"
    actions   = ["sns:Publish"]
    resources = ["*"]
  }

  dynamic "statement" {
    # The contents of the list below are arbitrary, but must be of length one.
    for_each = var.coordinator_b_assume_role_arn != var.coordinator_a_assume_role_arn ? [1] : []

    content {
      effect    = "Allow"
      actions   = ["sts:AssumeRole"]
      resources = [var.coordinator_b_assume_role_arn]
    }
  }
}

resource "aws_iam_role_policy" "enclave_role_policy" {
  policy = data.aws_iam_policy_document.enclave_policy_doc.json
  role   = aws_iam_role.enclave_role.id
}
