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

# Stores the queue name in SSM Parameter store for the worker to read
resource "aws_ssm_parameter" "worker_parameter" {
  name  = format("/aggregate-service/%s/%s", var.environment, var.parameter_name)
  type  = "String"
  value = var.parameter_value
  tags = {
    name        = format("/aggregate-service/%s/%s", var.environment, var.parameter_name)
    service     = "aggregate-service"
    environment = var.environment
  }
}
