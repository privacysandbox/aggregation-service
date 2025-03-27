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

resource "aws_dynamodb_table" "metadata_db_table" {
  name = var.table_name

  # Primary key information
  hash_key = "JobKey"
  attribute {
    name = "JobKey"
    type = "S"
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
    service     = "operator-service"
    environment = var.environment
    role        = "metadatadb"
  }
}

# Stores the table name in SSM Parameter store for the worker to read
resource "aws_ssm_parameter" "job_metadata_db_parameter_store" {
  name  = format("scp-%s-JOB_METADATA_DB", var.environment)
  type  = "String"
  value = var.table_name
  tags = {
    name        = format("scp-%s-JOB_METADATA_DB", var.environment)
    service     = "operator-service"
    environment = var.environment
    role        = "metadatadb"
  }
}

# Deprecated and replaced by "job_metadata_db_parameter_store", used only for
# backwards compatibility
resource "aws_ssm_parameter" "metadata_db_table_name_parameter_store_legacy" {
  name  = format("/aggregate-service/%s/dynamodb_metadatadb_table_name", var.environment)
  type  = "String"
  value = var.table_name
  tags = {
    name        = format("/aggregate-service/%s/dynamodb_metadatadb_table_name", var.environment)
    service     = "aggregate-service"
    environment = var.environment
    role        = "metadatadb"
  }
  overwrite = true
}
