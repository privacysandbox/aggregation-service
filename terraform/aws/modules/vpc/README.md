# Virtual Private Cloud (VPC)

## Context

As part of security hardening and best practices, it's recommended to route all application
communication through the internal AWS network via the use of VPC and VPC endpoint (VPCe).
Furthermore, enabling customer-owned networking provides flexibility supporting custom security
groups, policies and routes.

## Resources

This VPC module creates a tiered subnet layout, including the following:

-   Public subnets is used to contain resources that are exposed to the internet, such as a NAT
    gateway for the private subnets.
-   Private subnet is reserved for internal hosts, such as any EC2 instances, including the worker
    enclave. It uses a shared ACL permitting all communication by default. Instance in this subnet
    only has a private IP address but can communicate with public internet via a NAT gateway or
    internal resources such as DynamoDb through the internal network.
-   Spare capacity is not associated with any subnets and may be used for creating additional
    subnets if needed in the future

In addition to the subnet, this module also creates VPC endpoints, ACLs, security groups, providing
secure connections for internal and external services. For example, it creates gateway VPC service
endpoints to securely communicate with internal services including DynamoDb and S3 over the internal
AWS network. It also creates an internet gateway and NAT gateway to reach the Internet without
exposing a publicly routable IP address.
