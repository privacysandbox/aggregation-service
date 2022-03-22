# Demo Environment

This is a demo Terraform environment for the Aggregate Worker processes and can
be used as a reference for creating your own environment (e.g. "dev" and "prod")

## Synopsis

```sh
nano main.tf             # Add a Terraform state bucket!
nano example.auto.tfvars # Customize configuration
terraform init
terraform apply
```

## Configuration Files

The files which should be modified for your purposes for each environment you
create are:

- [example.auto.tfvars](./example.auto.tfvars) - an example configuation file
   for the aggregation serer process.
- [main.tf](./main.tf) - contains terraform provider information and state
   bucket location - shoudl be edited to point to a state bucket you control.

## Shared Files

The following files are copied from the [shared](../shared) folder and are
**not** intended to be modified:

- aggregation_service.tf
- aggregation_service_variables.tf
- ami_params.auto.tfvars

If your system supports symbolic links, it is recommended that you create a
symlink to each of these files in the shared folder rather than copying them.

*Modifying these files directly can cause issues when upgrading to newer
releases.*
