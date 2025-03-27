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

terraform {
  required_version = "~> 1.2.3"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.0"
    }
  }
}

data "aws_region" "current" {}

# Get all available AZs in the current AWS region
data "aws_availability_zones" "avail" {
  state = "available"
}

# This data source is used to get details about a specific AZ, such as its `name_suffix`
data "aws_availability_zone" "all" {
  for_each = toset(data.aws_availability_zones.avail.names)

  name = each.key
}

locals {
  region = data.aws_region.current.name

  # A map of availability zone to name suffix, such as "us-west1a" to "a".  Used
  # for deciding which AZs to create subnets, taking into account of their availability
  # state and user specification. An AZ is only added to the map if the AZ is in
  # an available state and specified by an user using the `vpc_availability_zones` variable.
  availability_zones_to_name_suffix = {
    for az, az_info in data.aws_availability_zone.all : az => az_info.name_suffix
    if contains(var.vpc_availability_zones, az_info.name_suffix)
  }

  # A map of availability zone suffix to netnum, used for implementing a
  # consistent subnet CIDR to az scheme even if AZ availability changes over time.
  # az_suffix represents the letter identifier of an AWS AZ, such a of us-west1a
  # and the netnum represents which subnet CIDR block partition to be used.
  az_suffix_to_netnum = {
    a = 0
    b = 1
    c = 2
    d = 3
    e = 4
    f = 5
    g = 6
    h = 7
    # up to h for a total of 8 AZs
  }

  # cidrsubnets() creates a list of consecutive subnets with the specified
  # additional bits added to the base subnet mask.
  # The module defines the new subnet masks as follows:
  # - 4 new bits for private subnets (/20 with a /16 VPC, 4,094 addresses)
  # - 5 new bits for public subnets (/21 with a /16 VPC, 2,046 addresses)
  # - 6 new bits for custom subnets (/22 - not provisioned by this module, 1,028 addresses)
  # The subnets are built out of 8 larger netblocks to allow for AZ replication.
  subnets = [
    for cidr_block in cidrsubnets(var.vpc_cidr, 3, 3, 3, 3, 3, 3, 3, 3) : cidrsubnets(cidr_block, 1, 2, 3)
  ]

  vpc_default_tags = {
    Name = "operator-service-${var.environment}"
  }
}

################################################################################
# VPC, Subnets, Security Groups, and ACL
################################################################################

resource "aws_vpc" "main" {
  cidr_block = var.vpc_cidr

  # Note: Do not remove; Necessary to enable private DNS for aws_vpc_endpoint
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = local.vpc_default_tags
}

# Private Subnet
resource "aws_subnet" "private" {
  for_each = local.availability_zones_to_name_suffix

  vpc_id            = aws_vpc.main.id
  availability_zone = each.key
  cidr_block        = local.subnets[local.az_suffix_to_netnum[each.value]][0]
  tags = merge(
    {
      subnet_type = "private"
    },
    {
      availability_zone = each.key
    },
    local.vpc_default_tags
  )

  # If the IAM Role permissions are removed before Lambda is able to delete its
  # Hyperplane ENIs, the subnet/security groups deletions will continually fail.
  # Mark an explicit dependency so that Lambda execution role is preserved until
  # after the subnet/security group is deleted.
  depends_on = [
    aws_iam_role_policy_attachment.lambda_vpc_access_execution_role_attachment
  ]
}

# Public Subnet
resource "aws_subnet" "public" {
  for_each = local.availability_zones_to_name_suffix

  vpc_id            = aws_vpc.main.id
  availability_zone = each.key
  cidr_block        = local.subnets[local.az_suffix_to_netnum[each.value]][1]
  tags = merge(
    {
      subnet_type = "public"
    },
    {
      availability_zone = each.key
    },
    local.vpc_default_tags
  )
}

# Create a default security group for the VPC.
# Note that without creating additional rules, this will deny all inbound and
# outbound traffic to member hosts.
resource "aws_security_group" "default" {
  vpc_id      = aws_vpc.main.id
  description = "Default security group for VPC ${aws_vpc.main.id} - Managed by Terraform"
  tags        = local.vpc_default_tags
}

resource "aws_security_group" "internal" {
  name        = "allow_internal"
  vpc_id      = aws_vpc.main.id
  description = "VPC allowing unrestricted internal traffic for member hosts in VPC ${aws_vpc.main.id} - Managed by Terraform"
  tags        = local.vpc_default_tags
}

resource "aws_security_group_rule" "allow_all_from_internal" {
  type              = "ingress"
  protocol          = "all"
  from_port         = 0
  to_port           = 0
  cidr_blocks       = [aws_vpc.main.cidr_block]
  security_group_id = aws_security_group.internal.id

  # Necessary workaround to avoid recreation errors. See
  # https://github.com/hashicorp/terraform-provider-aws/issues/12420
  lifecycle {
    create_before_destroy = true
  }
}

# NB: Necessary for clients in the VPC to establish connections to interface type
# VPC endpoints such the KMS endpoint.
resource "aws_security_group_rule" "allow_ingress_https" {
  type              = "ingress"
  description       = "https access from VPC"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = [aws_vpc.main.cidr_block]
  security_group_id = aws_security_group.internal.id
  # Necessary workaround to avoid recreation errors. See
  # https://github.com/hashicorp/terraform-provider-aws/issues/12420
  lifecycle {
    create_before_destroy = true
  }
}

# Create an additional security group used to allow egress to the Internet.
resource "aws_security_group" "egress" {
  name        = "allow_egress"
  vpc_id      = aws_vpc.main.id
  description = "VPC allowing unrestricted outbound Internet egress for member hosts in VPC ${aws_vpc.main.id} - Managed by Terraform"
  tags        = local.vpc_default_tags
}

resource "aws_security_group_rule" "allow_egress_http" {
  type              = "egress"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "http access"
  security_group_id = aws_security_group.egress.id
  # Necessary workaround to avoid recreation errors. See
  # https://github.com/hashicorp/terraform-provider-aws/issues/12420
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "allow_egress_https" {
  type              = "egress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "https access"
  security_group_id = aws_security_group.egress.id
  # Necessary workaround to avoid recreation errors. See
  # https://github.com/hashicorp/terraform-provider-aws/issues/12420
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "allow_egress_dns" {
  type              = "egress"
  from_port         = 53
  to_port           = 53
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "dns via tcp access"
  security_group_id = aws_security_group.egress.id
  # Necessary workaround to avoid recreation errors. See
  # https://github.com/hashicorp/terraform-provider-aws/issues/12420
  lifecycle {
    create_before_destroy = true
  }
}

# DNS has always been designed to use both UDP and TCP port 53 from the start
# with UDP being the default, and fall back to using TCP when it is unable to
# communicate on UDP, typically when the packet size is too large to push through
# in a single UDP packet.
resource "aws_security_group_rule" "allow_egress_dns_udp" {
  type              = "egress"
  from_port         = 53
  to_port           = 53
  protocol          = "udp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "dns via udp access"
  security_group_id = aws_security_group.egress.id
  lifecycle {
    create_before_destroy = true
  }
}

################################################################################
# Internet and NAT Gateway
################################################################################

resource "aws_internet_gateway" "inet_gateway" {
  vpc_id = aws_vpc.main.id
  tags   = local.vpc_default_tags
}

resource "aws_route" "inet_gateway_route" {
  route_table_id         = aws_vpc.main.default_route_table_id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.inet_gateway.id
}

# Create Elastic IPs for the NAT Gateway(s).
resource "aws_eip" "elastic_ip" {
  for_each = aws_subnet.public
  vpc      = true
}

# Create NAT gateway(s) in public subnets to allow egress from Private subnet.
# The current version of the AWS G3 TF provider does not support private NAT.
resource "aws_nat_gateway" "nat_gateway" {
  for_each = aws_subnet.public

  # The NAT Gateway is created in the public subnet to provide access to
  # private subnets, which route to it as a default gateway.
  subnet_id     = each.value.id
  allocation_id = aws_eip.elastic_ip[each.key].id
  depends_on    = [aws_internet_gateway.inet_gateway]
  tags = merge(
    {
      subnet_type = "public"
    },
    {
      availability_zone = each.key
    },
    local.vpc_default_tags
  )
}

################################################################################
# Routes
################################################################################

# Create subnet route tables for Private Subnet, to enable NAT Gateway routing.
resource "aws_route_table" "private_route_table" {
  for_each = aws_subnet.private

  vpc_id = aws_vpc.main.id
  tags = merge(
    {
      subnet_type = "private"
    },
    {
      availability_zone = each.key
    },
    local.vpc_default_tags
  )
}

resource "aws_route_table_association" "private_route_assoc" {
  for_each       = aws_subnet.private
  subnet_id      = each.value.id
  route_table_id = aws_route_table.private_route_table[each.key].id
}

# Add a default route to the NAT gateway attached to each public subnet.
resource "aws_route" "nat_gateway_route" {
  for_each = aws_route_table.private_route_table

  route_table_id         = each.value.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.nat_gateway[each.key].id
}

resource "aws_vpc_endpoint_route_table_association" "private_to_keydb_route" {
  for_each        = aws_route_table.private_route_table
  vpc_endpoint_id = aws_vpc_endpoint.dynamodb_endpoint.id
  route_table_id  = each.value.id
}

resource "aws_vpc_endpoint_route_table_association" "private_to_s3_route" {
  for_each = aws_route_table.private_route_table

  vpc_endpoint_id = aws_vpc_endpoint.s3_endpoint.id
  route_table_id  = each.value.id
}

################################################################################
# Lambda Role Policy Attachments
# Policy attachment to Lambda role to access VPC execution; necessary for the lifecycle
# a Lambda, including attaching to a custom VPC and deleting leftover ENIs.
# See https://docs.aws.amazon.com/lambda/latest/dg/configuration-vpc.html#vpc-permissions
################################################################################
resource "aws_iam_role_policy_attachment" "lambda_vpc_access_execution_role_attachment" {
  for_each = { for i, id in var.lambda_execution_role_ids : i => id }

  role       = each.value
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}
