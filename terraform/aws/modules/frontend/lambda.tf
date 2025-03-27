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


resource "aws_s3_bucket_object" "frontend_lambdas_package" {
  bucket      = aws_s3_bucket.lambda_package_storage.id
  key         = "app/frontend_lambdas.jar"
  source      = var.frontend_lambda_local_jar
  source_hash = filemd5(var.frontend_lambda_local_jar)
}
// IAM role the change handler will run with
resource "aws_iam_role" "frontend_lambda_role" {
  name = var.frontend_lambda_role_name
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

// Attachment for logging policy to change handler role
resource "aws_iam_role_policy_attachment" "frontend_lambda_log_policy_attachment" {
  policy_arn = aws_iam_policy.lambda_log_policy.arn
  role       = aws_iam_role.frontend_lambda_role.name
}

# Define lambda configurations
resource "aws_lambda_function" "get_job_lambda" {
  function_name = var.get_job_lambda_name

  s3_bucket = aws_s3_bucket_object.frontend_lambdas_package.bucket
  s3_key    = aws_s3_bucket_object.frontend_lambdas_package.key

  source_code_hash = filebase64sha256(aws_s3_bucket_object.frontend_lambdas_package.source)

  handler     = var.get_job_lambda_handler
  runtime     = "java11"
  timeout     = var.get_job_lambda_timeout
  memory_size = var.get_job_lambda_size

  role = aws_iam_role.frontend_lambda_role.arn

  depends_on = [
    aws_cloudwatch_log_group.get_job_lambda_cloudwatch,
  ]

  environment {
    variables = {
      MAX_WINDOW_AGE     = var.max_window_age,
      JOB_METADATA_TABLE = var.metadata_db_table_name,
      JOB_METADATA_TTL   = var.job_metadata_ttl
    }
  }

  publish = true
}

resource "aws_lambda_function" "create_job_lambda" {
  function_name = var.create_job_lambda_name

  s3_bucket = aws_s3_bucket_object.frontend_lambdas_package.bucket
  s3_key    = aws_s3_bucket_object.frontend_lambdas_package.key

  source_code_hash = filebase64sha256(aws_s3_bucket_object.frontend_lambdas_package.source)

  handler     = var.create_job_lambda_handler
  runtime     = "java11"
  timeout     = var.create_job_lambda_timeout
  memory_size = var.create_job_lambda_size

  role = aws_iam_role.frontend_lambda_role.arn

  depends_on = [
    aws_cloudwatch_log_group.create_job_lambda_cloudwatch,
  ]

  environment {
    variables = {
      MAX_WINDOW_AGE     = var.max_window_age,
      JOB_METADATA_TABLE = var.metadata_db_table_name,
      JOB_METADATA_TTL   = var.job_metadata_ttl
    }
  }

  publish = true
}

# Define permissions for this specific lambda
resource "aws_iam_role_policy" "frontend_policy" {
  name = "frontend_policy"
  role = aws_iam_role.frontend_lambda_role.id
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        Action : [
          "dynamodb:BatchGet*",
          "dynamodb:DescribeStream",
          "dynamodb:DescribeTable",
          "dynamodb:Get*",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:BatchWrite*",
          "dynamodb:Update*",
          "dynamodb:PutItem"
        ],
        Resource : var.metadata_db_arn
        Effect : "Allow"
      }
    ]
  })
}

# Cloudwatch permissions
resource "aws_cloudwatch_log_group" "get_job_lambda_cloudwatch" {
  name              = "/aws/lambda/${var.get_job_lambda_name}"
  retention_in_days = var.frontend_cloudwatch_retention_days
}

resource "aws_cloudwatch_log_group" "create_job_lambda_cloudwatch" {
  name              = "/aws/lambda/${var.create_job_lambda_name}"
  retention_in_days = var.frontend_cloudwatch_retention_days
}

# Provisioned concurrency
resource "aws_lambda_provisioned_concurrency_config" "get_job_lambda_provisioned_concurrency" {
  count                             = var.get_job_lambda_provisioned_concurrency_enabled ? 1 : 0
  function_name                     = aws_lambda_function.get_job_lambda.function_name
  provisioned_concurrent_executions = var.get_job_lambda_provisioned_concurrency_count
  qualifier                         = aws_lambda_function.get_job_lambda.version
}

resource "aws_lambda_provisioned_concurrency_config" "create_job_lambda_provisioned_concurrency" {
  count                             = var.create_job_lambda_provisioned_concurrency_enabled ? 1 : 0
  function_name                     = aws_lambda_function.create_job_lambda.function_name
  provisioned_concurrent_executions = var.create_job_lambda_provisioned_concurrency_count
  qualifier                         = aws_lambda_function.create_job_lambda.version
}

# Needed to actually route to version using provisioned concurrency
data "aws_lambda_function" "get_job_lambda_versioned_data_source" {
  count         = var.get_job_lambda_provisioned_concurrency_enabled ? 1 : 0
  function_name = aws_lambda_function.get_job_lambda.function_name
  qualifier     = aws_lambda_function.get_job_lambda.version
}

data "aws_lambda_function" "create_job_lambda_versioned_data_source" {
  count         = var.create_job_lambda_provisioned_concurrency_enabled ? 1 : 0
  function_name = aws_lambda_function.create_job_lambda.function_name
  qualifier     = aws_lambda_function.create_job_lambda.version
}
