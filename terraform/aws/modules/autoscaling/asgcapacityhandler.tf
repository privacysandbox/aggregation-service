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
resource "aws_s3_bucket_object" "asg_capacity_handler_lambda_s3_jar" {
  bucket      = aws_s3_bucket.lambda_package_storage.id
  key         = "app/asg_capacity_handler_lambda.jar"
  source      = var.asg_capacity_handler_lambda_local_jar
  source_hash = filemd5(var.asg_capacity_handler_lambda_local_jar)
}

// IAM role for autoscaling capacity lambda
resource "aws_iam_role" "asg_capacity_lambda_role" {
  name = var.asg_capacity_lambda_role_name
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

// IAM policy to get attributes for the SQS Job Queue
resource "aws_iam_policy" "sqs_get_attribute_access" {
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Action" : [
          "sqs:GetQueueAttributes"
        ],
        "Effect" : "Allow",
        "Resource" : var.jobqueue_sqs_arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "sqs_get_attribute_policy_attachment" {
  policy_arn = aws_iam_policy.sqs_get_attribute_access.arn
  role       = aws_iam_role.asg_capacity_lambda_role.name
}

resource "aws_iam_role_policy_attachment" "asg_capacity_lambda_log_policy_attachment" {
  policy_arn = aws_iam_policy.lambda_log_policy.arn
  role       = aws_iam_role.asg_capacity_lambda_role.name
}

// IAM policy to allow autoscaling group API calls
resource "aws_iam_policy" "asg_capacity_autoscaling_group_policy" {
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : [
          "autoscaling:SetDesiredCapacity"
        ],
        "Resource" : aws_autoscaling_group.worker_group.arn
      },
      {
        "Effect" : "Allow",
        "Action" : [
          "autoscaling:DescribeAutoScalingGroups",
        ],
        "Resource" : "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "autoscaling_group_policy_attachment" {
  policy_arn = aws_iam_policy.asg_capacity_autoscaling_group_policy.arn
  role       = aws_iam_role.asg_capacity_lambda_role.name
}

resource "aws_lambda_function" "asg_capacity_lambda" {
  function_name = var.asg_capacity_handler_lambda_name
  role          = aws_iam_role.asg_capacity_lambda_role.arn

  // Package location and handler method to call
  s3_bucket        = aws_s3_bucket.lambda_package_storage.id
  s3_key           = aws_s3_bucket_object.asg_capacity_handler_lambda_s3_jar.key
  source_code_hash = filebase64sha256(var.asg_capacity_handler_lambda_local_jar)
  handler          = var.asg_capacity_handler_method

  // Runtime parameters
  runtime     = "java11"
  memory_size = var.asg_capacity_handler_memory_size
  timeout     = var.asg_capacity_handler_lambda_runtime_timeout
  environment {
    variables = {
      AWS_SQS_URL   = var.jobqueue_sqs_url,
      ASG_NAME      = aws_autoscaling_group.worker_group.name,
      SCALING_RATIO = var.worker_scaling_ratio
    }
  }

  tags = {
    name        = var.asg_capacity_handler_lambda_name
    service     = "operator-service"
    environment = var.environment
    role        = "asg-capacity-handler"
  }
}

resource "aws_cloudwatch_event_rule" "asg_capacity_cron" {
  name                = "${var.environment}-asg-capacity-cron"
  description         = "Cron to run the ASG capacity lambda"
  schedule_expression = "rate(1 minute)"
}

resource "aws_cloudwatch_event_target" "asg_capacity_cron_target" {
  target_id = "asg-capacity-target"
  rule      = aws_cloudwatch_event_rule.asg_capacity_cron.name
  arn       = aws_lambda_function.asg_capacity_lambda.arn
}

resource "aws_lambda_permission" "asg_capacity_lambda_permission" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.asg_capacity_lambda.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.asg_capacity_cron.arn
}
