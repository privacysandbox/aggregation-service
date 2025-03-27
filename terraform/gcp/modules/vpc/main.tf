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
locals {
  egress_internet_tag = "egress-internet"
}

# Dedicated VPC network.
module "vpc_network" {
  source  = "terraform-google-modules/network/google"
  version = "~> 4.0"

  project_id   = var.project_id
  network_name = "${var.environment}-network"

  # Unlike AWS, concept of private/public subnets do not translate well to GCP
  # because routes to internet gateway are managed at the VPC network level.
  # Such that the route either exists for all subnets or not. GCP provides more
  # direct control of internet access like restricting route to internet gateway
  # to specific instance tags.
  #
  # Subnets will be created within the 10.128.0.0/9 range. Each subnet is /20
  # with 4096 addresses.
  auto_create_subnetworks = true # Don't need all subnets, but auto-create simplifies TF config.
  subnets                 = []   # Required argument.

  # Routes for each subnet are automatically created. Delete the default internet
  # gateway route and replace it with one restricted to the
  # `egress_internet_tag`.
  delete_default_internet_gateway_routes = true
  routes = [
    {
      name              = "${var.environment}-egress-internet"
      description       = "Route to the Internet."
      destination_range = "0.0.0.0/0"
      tags = join(",", [
        local.egress_internet_tag,
        "vpc-connector" # Tags of Serverless VPC connectors.
      ])
      next_hop_internet = "true"
    },
  ]
}

# Cloud NAT to provide internet to VMs without external IPs.
module "vpc_nat" {
  source  = "terraform-google-modules/cloud-nat/google"
  version = "~> 1.2"

  for_each = var.regions

  project_id    = var.project_id
  network       = module.vpc_network.network_self_link
  region        = each.value
  create_router = true
  router        = "${var.environment}-router"
}
