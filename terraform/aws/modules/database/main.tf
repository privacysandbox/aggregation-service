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

resource "aws_dynamodb_table" "dynamodb_table" {
  name = var.table_name

  # Primary key information
  hash_key = var.primary_key
  attribute {
    name = var.primary_key
    type = var.primary_key_type
  }

  ttl {
    attribute_name = "Ttl"
    enabled        = true
  }

  billing_mode   = "PROVISIONED"
  write_capacity = var.write_capacity
  read_capacity  = var.read_capacity

  # Streams configuration, stream ARN is in the module outputs
  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  point_in_time_recovery {
    enabled = var.enable_dynamo_point_in_time_recovery
  }

  # Tags for identifying the table in various metrics (billing, cloudwatch, etc)
  tags = {
    name        = var.table_name
    service     = var.service_tag
    environment = var.environment
    role        = var.role_tag
  }
}
