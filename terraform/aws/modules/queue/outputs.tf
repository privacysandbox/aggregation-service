/**
 * Copyright 2023 Google LLC
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

output "queue_sqs_url" {
  value       = aws_sqs_queue.queue.id
  description = "The URL of the SQS queue"
}

output "queue_sqs_arn" {
  value       = aws_sqs_queue.queue.arn
  description = "The ARN of the SQS queue"
}
