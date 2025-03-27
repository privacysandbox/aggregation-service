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

// s3 object for the lambda's jar
resource "aws_s3_bucket_object" "terminated_instance_handler_lambda_s3_jar" {
  bucket      = aws_s3_bucket.lambda_package_storage.id
  key         = "app/terminated_instance_handler_lambda.jar"
  source      = var.terminated_instance_handler_lambda_local_jar
  source_hash = filemd5(var.terminated_instance_handler_lambda_local_jar)
}

// IAM role for terminated instance lambda
resource "aws_iam_role" "terminated_instance_lambda_role" {
  name = var.terminated_instance_lambda_role_name
  assume_role_policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Principal" : {
          "Service" : "lambda.amazonaws.com"
        },
        "Action" : "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "terminated_instance_lambda_log_policy_attachment" {
  policy_arn = aws_iam_policy.lambda_log_policy.arn
  role       = aws_iam_role.terminated_instance_lambda_role.name
}

// IAM policy to allow autoscaling group API calls
resource "aws_iam_policy" "terminated_instance_autoscaling_group_policy" {
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : [
          "autoscaling:CompleteLifecycleAction",
        ],
        "Resource" : aws_autoscaling_group.worker_group.arn
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "autoscaling:DescribeAutoScalingInstances"
        ],
        "Resource" : "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "terminated_instance_autoscaling_group_policy_attachment" {
  policy_arn = aws_iam_policy.terminated_instance_autoscaling_group_policy.arn
  role       = aws_iam_role.terminated_instance_lambda_role.name
}

// IAM policy to allow access to AsgInstances DDB table.
resource "aws_iam_policy" "terminated_instance_asginstances_db_policy" {
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        Action : [
          "dynamodb:DescribeTable",
          "dynamodb:Get*",
          "dynamodb:Query",
          "dynamodb:Update*",
          "dynamodb:PutItem"
        ],
        Resource : var.asginstances_db_arn
        Effect : "Allow"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "terminated_instance_asginstances_db_policy_attachment" {
  policy_arn = aws_iam_policy.terminated_instance_asginstances_db_policy.arn
  role       = aws_iam_role.terminated_instance_lambda_role.name
}

resource "aws_lambda_function" "terminated_instance_lambda" {
  function_name = var.terminated_instance_handler_lambda_name
  role          = aws_iam_role.terminated_instance_lambda_role.arn

  // Package location and handler method to call
  s3_bucket        = aws_s3_bucket.lambda_package_storage.id
  s3_key           = aws_s3_bucket_object.terminated_instance_handler_lambda_s3_jar.key
  source_code_hash = filebase64sha256(var.terminated_instance_handler_lambda_local_jar)
  handler          = var.terminated_instance_handler_method

  // Runtime parameters
  runtime     = "java11"
  memory_size = var.terminated_instance_handler_memory_size
  timeout     = var.terminated_instance_handler_lambda_runtime_timeout
  environment {
    variables = {
      ASG_INSTANCES_DYNAMO_TABLE_NAME = var.asginstances_db_table_name,
      ASG_INSTANCES_DYNAMO_TTL_DAYS   = var.asginstances_db_ttl_days
    }
  }

  tags = {
    name        = var.terminated_instance_handler_lambda_name
    service     = "operator-service"
    environment = var.environment
    role        = "terminated-instance-handler"
  }
}

resource "aws_cloudwatch_event_rule" "terminated_instance_event_rule" {
  name        = "${var.environment}-terminated_instance_rule"
  description = "Event rule to trigger on worker instance termination"
  event_pattern = jsonencode({
    "source" : ["aws.autoscaling"],
    "detail-type" : ["EC2 Instance-terminate Lifecycle Action"],
    "detail" : {
      "AutoScalingGroupName" : [
        aws_autoscaling_group.worker_group.name
      ],
      "LifecycleHookName" : [
        aws_autoscaling_lifecycle_hook.worker_scale_in_hook.name
      ]
    }
  })
}

resource "aws_cloudwatch_event_target" "terminated_instance_event_target" {
  target_id = "terminated-instance-target"
  rule      = aws_cloudwatch_event_rule.terminated_instance_event_rule.name
  arn       = aws_lambda_function.terminated_instance_lambda.arn
}

resource "aws_lambda_permission" "terminated_instance_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.terminated_instance_lambda.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.terminated_instance_event_rule.arn
}
