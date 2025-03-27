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

output "db_stream_arn" {
  value       = aws_dynamodb_table.dynamodb_table.stream_arn
  description = "The ARN of the DynamoDB stream for the table"
}

output "db_arn" {
  value       = aws_dynamodb_table.dynamodb_table.arn
  description = "The ARN of the DynamoDB table"
}

output "db_table_name" {
  value       = aws_dynamodb_table.dynamodb_table.id
  description = "The name of the DynamoDB table"
}
