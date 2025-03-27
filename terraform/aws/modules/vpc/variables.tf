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

################################################################################
# Global Variables
################################################################################

variable "environment" {
  description = "Description for the environment, e.g. dev, staging, production."
  type        = string
}

################################################################################
# VPC Variables
################################################################################

variable "vpc_cidr" {
  description = "VPC CIDR range for coordinator VPC. Needs to be a CIDR block size of at least /26."
  type        = string
}

variable "vpc_availability_zones" {
  description = "Specify the letter identifiers of which availability zones to deploy resources, such as a, b and c."
  type        = set(string)
}

variable "lambda_execution_role_ids" {
  description = "IDs of the lambda execution roles needed for VPC access."
  type        = list(string)
}

################################################################################
# VPC Endpoint Variables
################################################################################

# Note: principal_arns refer to the principal that made
# the request with the ARN specified in the policy. For IAM roles, it refers to
# the ARN of the role, not the ARN of the user that assumed the role. For example,
# Lambda or EC2 principal arns are specified as arn:aws:iam::<123456789012>:role/<role-name>.
# See AWS doc: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_identifiers.html
variable "dynamodb_allowed_principal_arns" {
  description = "ARNs of allowed principals to access DynamoDb VPCe."
  type        = list(string)
}

variable "dynamodb_arns" {
  description = "ARN of the existing DynamoDb table name used by the DynamoDb VPC endpoint."
  type        = list(string)
}

variable "s3_allowed_principal_arns" {
  description = "ARNs of allowed principals to access DynamoDb VPCe."
  type        = list(string)
}
