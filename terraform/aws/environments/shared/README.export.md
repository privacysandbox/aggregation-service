# Overview

Welcome to the README for setting up the Operator Service on AWS!

This document will walk through deploying an operator environment using Terraform and can be used as
a reference for creating your own environment ( e.g. "dev" and "prod")

## Pre-requisites

If not already done, setup Terraform and AWS credentials:

1. Install a compatible version of Terraform (>= v1.2.3) [here](https://www.terraform.io/downloads)
1. Configure AWS credentials using
   [AWS documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html)

You should notice that the directory contains the following files:

```sh
? tree
.
+-- BUILD
+-- example.auto.tfvars
+-- main.tf
+-- operator_service.tf -> ../shared/operator_service.tf
+-- operator_service_variables.tf -> ../shared/operator_service_variables.tf
+-- README.md

0 directories, 6 files
```

Note `operator_service.tf`, `operator_service_variable.tf` and `README.md` are symlinked files that
should not be modified.

## Instructions

### Deploy a new infrastructure

1. Edit service configurations in [example.auto.tfvars](example.auto.tfvars) - an example
   configuration file to deploy an operator service, such as the following.

    ```sh
    coordinator_a_assume_role_parameter = "arn:aws:iam::<CoordinatorAAccount>:role/<CoordinatorARole>"
    coordinator_b_assume_role_parameter = "arn:aws:iam::<CoordinatorBAccount>:role/<CoordinatorBRole>"
    ```

    For a full list of configuration parameters, see `operator_service_variable.tf`. Most
    configuration variables are optional with the exception of `region` , `environment`, and
    `ami_name` as required variables. If you do not provide a value, the default values will be used
    for optional variables. You may override the default values by specifying custom values in the
    configuration file.

    > Note: You can provide your own VPC by setting `enable_user_provided_vpc` to true and
    > specifying the `user_provided_vpc_subnet_ids` and `user_provided_vpc_security_group_ids`
    > variable.

1. Edit terraform configuration [main.tf](main.tf) - contains terraform provider information and
   state bucket location to point to a remote state bucket that you control, such as the following

    We recommend you store the [Terraform state](https://www.terraform.io/language/state) in a cloud
    bucket. Create a S3 bucket via the console/cli, which we'll reference as `tf_state_bucket_name`.
    Consider enabling `versioning` to preserve, retrieve, and restore previous versions and set
    appropriate policies for this bucket to prevent accidental changes and deletion.

    ```sh
    terraform {
      backend "s3" {
        bucket = "<tf_state_bucket_name>"
        key    = "<new-environment.tfstate>"
        region = "<tf_state_bucket_aws_region>"
      }
    ```

1. Deploy the service by running the following commands

    ```sh
    > terraform init
    > terraform plan -out=tfplan # review the changes via terraform show tfplan
    > terraform apply tfplan
    ```

### Update an existing infrastructure

1. Plan and review changes to an existing infrastructure by running the following commands.

    ```sh
    > terraform init
    > terraform plan -out=tfplan # review the changes via terraform show tfplan
    ```

    If you observe changes to the autoscaler group `vpc_zone_identifiers`, such as shown below:

    ```sh
      # module.operator_service.module.worker_autoscaling.aws_autoscaling_group.worker_group will be updated in-place
      ~ resource "aws_autoscaling_group" "worker_group" {
            id                        = "operator-aggregation-service-workers"
            name                      = "operator-aggregation-service-workers"
          ~ vpc_zone_identifier       = [
              - "subnet-01b831b5c1519f756",
              - "subnet-01ff0807c98771430",
              - "subnet-03bb1e4f058bef3ad",
              - "subnet-054594e75098800df",
              - "subnet-0ab71ab5ea0a4fd43",
              - "subnet-0d370f0492a306a04",
            ] -> (known after apply)
            # (21 unchanged attributes hidden)
        }
    ```

    Go to step 2.1, otherwise go to step 2.2.

    > Note: This situation may occur if you are updating an existing operator from an already
    > associated VPC to a new VPC, such as providing your own VPC or updating the VPC deployment.

1.  1. If there are updates to the existing VPC, you are recommended to scale down the ASG group to
       0 capacity before proceeding with the upgrade. You can do so manually via the AWS console or
       CLI.

        Use this script [upgrade_operator_vpc](../../util_scripts/deploy/upgrade_operator_vpc) that
        automates the process. Skip 2b if you run the script.

    1. If there are no changes to the autoscaler group `vpc_zone_identifiers`, you may proceed with
       the normal Terraform apply operation by running `terraform apply tfplan`. If you encounter
       issues during the update process, please reference the Troubleshooting section.

## Troubleshooting

### **Issue**: Failure in VPC output precondition

```sh
| Error: Module output value precondition failed
|
|   on ../../modules/vpc/outputs.tf line 22, in output "vpc_id":
|   22:     condition     = length(local.availability_zones_to_name_suffix) > 0
|     +----------------
|     | local.availability_zones_to_name_suffix is object with no attributes
|
| Output must include at least one subnet.  Check inputs to vpc_availability_zones.
```

**Solution**: This means that the VPC module did not create any valid subnets. Check inputs to the
`vpc_availability_zones` variable.

### **Issue**: Unsupported instance type in AZ

```sh
?
| Error: Error creating Auto Scaling Group: ValidationError: You must use a valid fully-formed launch template. Your requested instance type (m5.2xlarge) is not supported in your requested Availability Zone (us-east-1e). Please retry your request by not specifying an Availability Zone or choosing us-east-1a, us-east-1b, us-east-1c, us-east-1d, us-east-1f.
|   status code: 400, request id: 9ff2b6ed-887f-4158-88a8-898d3573980e
|
|   with module.aggregate_service.module.worker_autoscaling.aws_autoscaling_group.worker_group,
|   on services/autoscaling/main.tf line 21, in resource "aws_autoscaling_group" "worker_group":
|   21: resource "aws_autoscaling_group" "worker_group" {
```

**Solution**: This means the instance type `m5.2xlarge` is not supported in the us-east-1e
availability zone. You may exclude invalid zones from VPC creation by using the
`vpc_availability_zones` variable.

### **Issue**: Failure deleting subnets during VPC update

```sh
module.vpc.aws_vpc.this: Destroying... (ID: vpc-0626034ed6648e300)
module.vpc.aws_subnet.foo-b: Still destroying... (ID: vpc-0626034ed6648e300, 20mins elapsed)
module.vpc.aws_subnet.foo-b: Still destroying... (ID: vpc-0626034ed6648e300, 20mins elapsed)
...

aws_subnet.foo-b: Error deleting subnet: timeout while waiting
  for state to become 'destroyed' timed out after 20 mins
aws_vpc.this: DependencyViolation: The vpc 'vpc-0626034ed6648e300' has dependencies and cannot be deleted.
  status code: 400, request id: 40cfa5b7-4a3f-418c-9f2f-8fa450dda882
```

This issue can occur if you are updating an operator service with a bring-your-own VPC or the new
secure VPC module for the first time. This issue occurs because old instances in the autoscaler
group are not completely terminated. Therefore, the subnets associated with these instances cannot
be deleted.

**Solution**:

1. Wait until autoscaling group instance refresh completes with any instances responding to a
   scale-in event in the terminated state. If any instances are stuck in a `terminating:wait` state,
   the default timeout is typically an hour before they are terminated. To check the status of the
   instance refresh, see
   [AWS Documentation](https://docs.aws.amazon.com/autoscaling/ec2/userguide/check-status-instance-refresh.html)
   .

    - [Optional] you can manually
      [complete a lifecycle action](https://docs.aws.amazon.com/autoscaling/ec2/userguide/completing-lifecycle-hooks.html#completing-lifecycle-hooks-aws-cli)
      of any instances in a wait state, using the following sample command.

        ```sh
        aws autoscaling complete-lifecycle-action --lifecycle-action-result CONTINUE \
          --lifecycle-hook-name my-launch-hook --auto-scaling-group-name my-asg \
          --lifecycle-action-token bcd2f1b8-9a78-44d3-8a7a-4dd07d7cf635
        ```

1. Run `terraform apply` using the latest states

### **Issue**: Unable to access worker instances via SSH or EC2 Instance Connect

If you see the following error message connecting to a worker instance via EC2 Instance Connect
using the AWS web console:

```sh
The instance does not have a public IPv4 address
To connect using the EC2 Instance Connect browser-based client, the instance must have a public IPv4 address.
```

This means that the instance lives in a private subnet with no public IP address assigned.

**Solution**:

On the AWS console, you can use the Session Manager tab to connect to the instance. After clicking
on the instance, you can go connect -> session manager -> connect. You should see a new browser tab
showing a terminal to the instance.

Alternatively, you may install the Session Manager plugin for the AWS cli to start and end a session
to any instance directly on your console instead of AWS web console. See detailed instructions
[here](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with.html).
