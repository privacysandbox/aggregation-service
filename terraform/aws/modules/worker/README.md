# Aggregation Service Worker Terraform Module

Creates group of ec2 instances running the aggregation service.

## Usage

Given an AMI file with an enclave image file, this module starts up an enclave and runs the enclave
image file in a group of ec2 instances. By default, this module with launch a single ec2 instance
without autoscaling enabled.

See `//terraform/aws/services/worker/environmets/dev` for an example of how to run this module and
`variables.tf` for a complete list of inputs to the module.

Note: Make sure the AMI you provide has an `.eif` file containing the enclave image under the
`/home/ec2-user` directory.
