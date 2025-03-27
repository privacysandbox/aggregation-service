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
resource "aws_s3_bucket_object" "change_handler_lambda_s3_jar" {
  bucket      = aws_s3_bucket.lambda_package_storage.id
  key         = "app/change_handler_lambda.jar"
  source      = var.change_handler_lambda_local_jar
  source_hash = filemd5(var.change_handler_lambda_local_jar)
}

// IAM role the change handler will run with
resource "aws_iam_role" "change_handler_lambda_role" {
  name = var.change_handler_lambda_role_name
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
resource "aws_iam_role_policy_attachment" "change_handler_lambda_log_policy_attachment" {
  policy_arn = aws_iam_policy.lambda_log_policy.arn
  role       = aws_iam_role.change_handler_lambda_role.name
}

// IAM policy to read from the Dynamo Metadta DB stream
resource "aws_iam_policy" "change_handler_metadata_db_stream_read_access" {
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : [
          "dynamodb:DescribeStream",
          "dynamodb:GetRecords",
          "dynamodb:GetShardIterator",
          "dynamodb:ListStreams"
        ],
        "Resource" : "${var.metadata_db_stream_arn}"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "change_handler_metadata_db_stream_read_access_attachment" {
  policy_arn = aws_iam_policy.change_handler_metadata_db_stream_read_access.arn
  role       = aws_iam_role.change_handler_lambda_role.name
}

// IAM policy to write to the SQS Job Queue
resource "aws_iam_policy" "change_handler_jobqueue_write_access" {
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Action" : [
          "sqs:GetQueueUrl",
          "sqs:SendMessage",
          "sqs:SendMessageBatch"
        ],
        "Effect" : "Allow",
        "Resource" : "${var.jobqueue_sqs_arn}"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "change_handler_jobqueue_write_access_attachment" {
  policy_arn = aws_iam_policy.change_handler_jobqueue_write_access.arn
  role       = aws_iam_role.change_handler_lambda_role.name
}

// The lambda for the change handler
resource "aws_lambda_function" "change_handler_lambda" {
  function_name = var.change_handler_lambda_name

  // Package location and handler method to call
  s3_bucket        = aws_s3_bucket.lambda_package_storage.id
  s3_key           = aws_s3_bucket_object.change_handler_lambda_s3_jar.key
  source_code_hash = filebase64sha256(var.change_handler_lambda_local_jar)
  handler          = var.change_handler_method

  role = aws_iam_role.change_handler_lambda_role.arn

  // Runtime parameters
  runtime     = "java11"
  memory_size = var.change_handler_memory_size
  timeout     = var.change_handler_lambda_runtime_timeout
  environment {
    variables = {
      AWS_SQS_URL = var.jobqueue_sqs_url
    }
  }

  tags = {
    name        = var.change_handler_lambda_name
    service     = "operator-service"
    environment = var.environment
    role        = "change-handler"
  }
}

// Configures the lambda to be triggered by the stream from the Metadata DB and
// add mishandled jobs to the SQS DLQ
resource "aws_lambda_event_source_mapping" "change_handler_metadata_db_stream_mapping" {
  event_source_arn  = var.metadata_db_stream_arn
  function_name     = aws_lambda_function.change_handler_lambda.arn
  starting_position = "LATEST"

  maximum_retry_attempts        = var.change_handler_lambda_maximum_retry_attempts
  maximum_record_age_in_seconds = var.change_handler_lambda_maximum_maximum_record_age_in_seconds

  // Split the batch if a failure occurs, isolating any "poison pill" entries
  bisect_batch_on_function_error = true

  // Write event data for mishandled jobs to the SQS DLQ
  destination_config {
    on_failure {
      destination_arn = aws_sqs_queue.change_handler_dead_letter_queue.arn
    }
  }
  depends_on = [aws_iam_role_policy.change_handler_dlq_write_access]
}

// SQS DLQ for mishandled jobs that need to be cleaned up by another lambda
resource "aws_sqs_queue" "change_handler_dead_letter_queue" {
  name = var.change_handler_dlq_sqs_name

  // 14 days, AWS maximum
  message_retention_seconds = var.change_handler_dlq_sqs_message_retention_seconds
  # Enable server-side encryption
  sqs_managed_sse_enabled = true

  // Tags for identifying the queue in various metrics
  tags = {
    name        = var.change_handler_dlq_sqs_name
    service     = "operator-service"
    environment = var.environment
    role        = "change-handler-dead-letter-queue"
  }
}

// IAM Role policy allowing mishandled jobs to be added to the SQS DLQ
resource "aws_iam_role_policy" "change_handler_dlq_write_access" {
  role = aws_iam_role.change_handler_lambda_role.name
  policy = jsonencode({
    Version : "2012-10-17",
    Statement : [
      {
        Action : [
          "sqs:GetQueueUrl",
          "sqs:SendMessage",
          "sqs:SendMessageBatch"
        ],
        Effect : "Allow",
        Resource : aws_sqs_queue.change_handler_dead_letter_queue.arn
      }
    ]
  })
}

resource "aws_s3_bucket_object" "dlq_cleanup_lambda_s3_jar" {
  bucket      = aws_s3_bucket.lambda_package_storage.id
  key         = "app/failed_job_queue_write_cleanup_lambda.jar"
  source      = var.cleanup_lambda_local_jar
  source_hash = filemd5(var.cleanup_lambda_local_jar)
}

// lambda function to handle cleanup when entries aren't written to the job
// processing queue
resource "aws_lambda_function" "dlq_cleanup_lambda" {
  function_name = var.cleanup_lambda_name

  s3_bucket        = aws_s3_bucket.lambda_package_storage.id
  s3_key           = aws_s3_bucket_object.dlq_cleanup_lambda_s3_jar.key
  source_code_hash = filebase64sha256(var.cleanup_lambda_local_jar)
  handler          = var.cleanup_lambda_method
  role             = aws_iam_role.dlq_cleanup_lambda_role.arn

  // Runtime parameters
  runtime     = "java11"
  memory_size = var.cleanup_lambda_memory_size
  timeout     = var.cleanup_lambda_runtime_timeout

  environment {
    variables = {
      JOB_METADATA_TABLE = var.metadata_db_table_name
      JOB_METADATA_TTL   = var.job_metadata_ttl
    }
  }
}

resource "aws_iam_role" "dlq_cleanup_lambda_role" {
  name = var.cleanup_lambda_role_name
  assume_role_policy = jsonencode({
    Version : "2012-10-17",
    Statement : [
      {
        Effect : "Allow",
        Principal : {
          Service : "lambda.amazonaws.com"
        },
        Action : "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "dlq_cleanup_lambda_dynamo_access" {
  role = aws_iam_role.dlq_cleanup_lambda_role.id
  policy = jsonencode({
    Version : "2012-10-17",
    Statement : [
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

resource "aws_iam_role_policy" "dlq_cleanup_lambda_dynamo_stream_access" {
  role = aws_iam_role.dlq_cleanup_lambda_role.id
  policy = jsonencode({
    Version : "2012-10-17",
    Statement : [
      {
        Effect : "Allow",
        Action : [
          "dynamodb:DescribeStream",
          "dynamodb:GetRecords",
          "dynamodb:GetShardIterator",
          "dynamodb:ListStreams"
        ],
        Resource : var.metadata_db_stream_arn
      }
    ]
  })
}

resource "aws_iam_role_policy" "dlq_cleanup_lambda_sqs_access" {
  role = aws_iam_role.dlq_cleanup_lambda_role.id
  policy = jsonencode({
    Version : "2012-10-17",
    Statement : [
      {
        Effect : "Allow",
        Action : [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
        ],
        Resource : aws_sqs_queue.change_handler_dead_letter_queue.arn
      }
    ]
  })
}

resource "aws_lambda_event_source_mapping" "dlq_cleanup_lambda_event_mapping" {
  event_source_arn = aws_sqs_queue.change_handler_dead_letter_queue.arn
  function_name    = aws_lambda_function.dlq_cleanup_lambda.arn
}

resource "aws_iam_role_policy_attachment" "dlq_cleanup_lambda_log_policy_attachment" {
  policy_arn = aws_iam_policy.lambda_log_policy.arn
  role       = aws_iam_role.dlq_cleanup_lambda_role.name
}
