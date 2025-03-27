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

output "vpc_id" {
  description = "ID of the newly created operator VPC."
  value       = aws_vpc.main.id

  precondition {
    condition     = length(local.availability_zones_to_name_suffix) > 0
    error_message = "Output must include at least one valid AZ.  Check inputs to vpc_availability_zones."
  }
}

output "public_subnet_ids" {
  description = "ID(s) of the newly created public subnets in the operator VPC."
  value       = [for s in aws_subnet.public : s.id]
}

output "public_subnet_cidrs" {
  description = "CIDR IP ranges of the newly created public subnets in the operator VPC."
  value       = [for s in aws_subnet.public : s.cidr_block]
}

output "private_subnet_ids" {
  description = "ID(s) of the newly created private subnets in the operator VPC."
  value       = [for s in aws_subnet.private : s.id]
}

output "private_subnet_cidrs" {
  description = "CIDR IP ranges of the newly created private subnets in the operator VPC."
  value       = [for s in aws_subnet.private : s.cidr_block]
}

output "vpc_default_sg_id" {
  description = "ID of the default security group in the newly created VPC."
  value       = aws_security_group.default.id
}

output "allow_egress_sg_id" {
  description = "ID of the allow_egress security group in the newly created VPC."
  value       = aws_security_group.egress.id
}

output "allow_internal_ingress_sg_id" {
  description = "ID of the allow_internal security group in the newly created VPC."
  value       = aws_security_group.internal.id
}

output "dynamodb_vpc_endpoint_id" {
  description = "ID of the VPC endpoint accessing DynamoDb."
  value       = aws_vpc_endpoint.dynamodb_endpoint.id
}

output "s3_vpc_endpoint_id" {
  description = "ID of the VPC endpoint accessing S3."
  value       = aws_vpc_endpoint.s3_endpoint.id
}
