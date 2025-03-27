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

resource "aws_dynamodb_table" "asginstances_db_table" {
  name = var.table_name

  # Primary key information
  hash_key = "InstanceName"
  attribute {
    name = "InstanceName"
    type = "S"
  }

  ttl {
    attribute_name = "Ttl"
    enabled        = true
  }

  billing_mode = "PAY_PER_REQUEST"

  point_in_time_recovery {
    enabled = true
  }

  # Tags for identifying the table in various metrics (billing, cloudwatch, etc)
  tags = {
    name        = var.table_name
    service     = "operator-service"
    environment = var.environment
    role        = "asginstancesdb"
  }
}

module "asg_instances_table_parameter" {
  source          = "../parameters"
  environment     = var.environment
  parameter_name  = "ASG_INSTANCES_TABLE_NAME"
  parameter_value = aws_dynamodb_table.asginstances_db_table.name
}
