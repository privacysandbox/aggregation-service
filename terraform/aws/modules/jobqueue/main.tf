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

resource "aws_sqs_queue" "job_queue" {
  name = var.queue_name

  # 14 days, AWS maximum
  message_retention_seconds = 1209600
  # Enable server-side encryption
  sqs_managed_sse_enabled = true

  # Tags for identifying the queue in various metrics
  tags = {
    name        = var.queue_name
    service     = "operator-service"
    environment = var.environment
    role        = "job_queue"
  }
}

# Stores the queue name in SSM Parameter store for the worker to read
resource "aws_ssm_parameter" "aws_job_queue_parameter_store" {
  name  = format("scp-%s-JOB_QUEUE", var.environment)
  type  = "String"
  value = aws_sqs_queue.job_queue.id
  tags = {
    name        = format("scp-%s-JOB_QUEUE", var.environment)
    service     = "operator-service"
    environment = var.environment
    role        = "job_queue"
  }
}

# Deprecated and replaced by "aws_job_queue_parameter_store", used only for
# backwards compatibility
resource "aws_ssm_parameter" "aws_sqs_queue_url_parameter_store_legacy" {
  name  = format("/aggregate-service/%s/sqs_queue_url", var.environment)
  type  = "String"
  value = aws_sqs_queue.job_queue.id
  tags = {
    name        = format("/aggregate-service/%s/sqs_queue_url", var.environment)
    service     = "aggregate-service"
    environment = var.environment
    role        = "job_queue"
  }
  overwrite = true
}
