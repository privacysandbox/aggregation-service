# `//terraform`

This contains terraform configuration for the infrastructure that ad-techs will run.

## Structure

-   `/services` contains parametarized resources that can be imported and deployed in various
    environments
-   `/environments` contains the definitions of the various environments for the application (e.g.
    dev, staging, production), importing the various service modules and providing parameters to
    them.

## Applying configuration

1. Select one of the environments: `cd environments/dev`
2. Init: `terraform init`
3. Plan: `terraform plan`
4. Apply: `terraform apply`
