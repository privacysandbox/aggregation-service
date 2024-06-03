# Testing on AWS using encrypted reports

## Prerequisites

To test the aggregation service with support for encrypted reports, you need the following:

-   Have an [AWS account](https://portal.aws.amazon.com/gp/aws/developer/registration/index.html)
    available to you.
-   Complete the aggregation service [onboarding form](https://forms.gle/EHoecersGKhpcLPNA)

Once you've submitted the onboarding form, we will contact you to verify your information. Then,
we'll send you the remaining instructions and information needed for this setup.</br> _You won't be
able to successfully setup your AWS system without completing the onboarding process!_

To set up aggregation service in AWS you'll use [Terraform](https://www.terraform.io/).

## Set up AWS client

Make sure you
[install](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) and
[set up](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html) the latest
AWS client.

## Set up Terraform

Change into the `<repository_root>/terraform/aws` folder. See
[clone the repository](/docs/local-testing-tool.md#clone-the-repository) if you have not cloned the
repository so far.

The setup scripts require Terraform version `1.2.3`. You can download Terraform version `1.2.3` from
[https://releases.hashicorp.com/terraform/1.2.3/](https://releases.hashicorp.com/terraform/1.2.3/)
or _at your own risk_, you can install and use
[Terraform version manager](https://github.com/tfutils/tfenv) instead.

If you have the Terraform version manager `tfenv` installed, run the following in your
`<repository_root>` to set Terraform to version `1.2.3`.

```sh
tfenv install 1.2.3;
tfenv use 1.2.3
```

We recommend you store the [Terraform state](https://www.terraform.io/language/state) in a cloud
bucket. Create a S3 bucket via the console/cli, which we'll reference as `tf_state_bucket_name`.
Consider enabling `versioning` to preserve, retrieve, and restore previous versions and set
appropriate policies for this bucket to prevent accidental changes and deletion.

## Download Terraform scripts and prebuilt dependencies

_Note: The prebuilt Amazon Machine Image (AMI) for the aggregation service is only available in the
`us-east-1` region. If you like to deploy the aggregation service in a different region you need to
copy the released AMI to your account or build it using our provided scripts._

If you like to build the Amazon Machine Image including the enclave container, as well as the Lambda
jars in your account, please follow the instructions in
[build-scripts/aws](/build-scripts/aws/README.md). This will skip running
`bash download_prebuilt_dependencies.sh` and run `bash fetch_terraform.sh` instead. Continue with
the [next deployment step](#set-up-your-deployment-environment) after building and downloading your
self-build jars.

The Terraform scripts to deploy the aggregation service depend on 5 packaged jars for Lambda
functions deployment. These jars are hosted on Amazon S3
(<https://aggregation-service-published-artifacts.s3.amazonaws.com/aggregation-service/{version}/{jar_file>})
and can be downloaded with the `<repository_root>/terraform/aws/download_prebuilt_dependencies.sh`
script. The script downloads the terrafrom scripts and jars which will be stored in
`<repository_root>/terraform/aws`. License information of downloaded dependencies can be found in
the [DEPENDENCIES.md](/DEPENDENCIES.md)

Run the following script in the `<repository_root>/terraform/aws` folder to download the prebuilt
dependencies.

```bash
bash download_prebuilt_dependencies.sh
```

_Note: The above script needs to be run with `bash` and does not support `sh`\*_

For manual download into the `<repository_root>/terraform/aws/jars` folder you can download them
from the links on our
[releases page](https://github.com/privacysandbox/aggregation-service/releases).

To copy the AMI to another account and/or region, follow the steps below.

1. Open the EC2 console at <https://console.aws.amazon.com/ec2/>.
2. In the navigation pane, choose AMI under images.
3. Select the AMI aggregation-service-enclave\_[VERSION]--[DATETIME]. It will be under "Public
   Images" category.
4. To copy the AMI, select the "Actions" -> "Copy AMI" for the selected AMI.
5. [Optional] Change the Destination Region if another one is preferred.
6. Click on "Copy AMI".

## Set up your deployment environment

We use the following folder structure
`<repository_root>/terraform/aws/environments/<environment_name>` to separate deployment
environments.

To set up your first environment (e.g `dev`), copy the `demo` environment. Run the following
commands from the `<repository_root>/terraform/aws/environments` folder:

```sh
mkdir dev
cp -R demo/* dev
cd dev
```

Make the following adjustments in the `<repository_root>/terraform/aws/environments/dev` folder:

1.  Add the `tf_state_bucket_name` to your `main.tf` by uncommenting and replacing the values using
    `<...>`:

    ```sh
    # backend "s3" {
    #   bucket = "<tf_state_bucket_name>"
    #   key    = "<environment_name>.tfstate"
    #   region = "us-east-1"
    # }
    ```

1.  Rename `example.auto.tfvars` to `<environment>.auto.tfvars` and add the `...assume_role...`
    values using the information you received in the onboarding email. Delete the line that reads
    `assume_role_parameter = "arn:aws:iam::example:role/example"` Leave all other values as-is for
    the initial deployment.

    ```sh
    environment = "<environment_name>"
    ...

    coordinator_a_assume_role_parameter = "arn:aws:iam::<CoordinatorAAccountID>:role/a_<YourAccountID>_coordinator_assume_role"
    coordinator_b_assume_role_parameter = "arn:aws:iam::<CoordinatorBAccountID>:role/b_<YourAccountID>_coordinator_assume_role"
    ...

    alarm_notification_email = "<noreply@example.com>"
    ```

    -   environment: name of your environment
    -   coordinator_a_assume_role_parameter: IAM role for Coordinator A given by us in the
        onboarding or upgrade email
    -   coordinator_b_assume_role_parameter: IAM role for Coordinator B given by us in the
        onboarding or upgrade email
    -   alarm_notification_email: Email to receive alarm notifications. Requires confirmation
        subscription through sign up email sent to this address.
    -   region: The region of the AMI. It is set to "us-east-1" by default. If the AMI is self-built
        or is copied to another region, then this value needs to be updated.

    Note: If you want to use an instance type other than the default one specified in the
    configuration, we recommend using an instance type with single NUMA node. Memory and CPUs for
    the enclave must be from the [same NUMA node](https://docs.kernel.org/virt/ne_overview.html);
    however, a single NUMA node on AWS EC2 has a maximum of 48 cores. Please refer to
    [sizing guidance](/docs/sizing-guidance.md) for instance type recommendations. For AWS instance
    specific questions, please contact AWS support.

1.  **Follow this step if you self-build your AMI and jars or copy the AMI (self-build or prebuilt
    AMI) to your account**

    If you [self-build your AMI and jars](/build-scripts/aws/README.md) or copied the AMI to your
    account, you need to copy the contents of the `release_params.auto.tfvars` file into a new file
    `self_build_params.auto.tfvars` remove the `release_params.auto.tfvars` file afterwards.

    To copy without symlink, run the following in the
    `<repository_root>/terraform/aws/environments/dev` folder

    ```sh
    cp -L release_params.auto.tfvars self_build_params.auto.tfvars
    ```

    Then delete the symlinked file:

    ```sh
    rm release_params.auto.tfvars
    ```

    And change the line `ami_owners = ["971056657085"]` to `ami_owners = ["self"]` in your
    `self_build_params.auto.tfvars`.

1.  Once you've adjusted the configuration, run the following in the
    `<repository_root>/terraform/aws/environments/dev` folder

    Install all Terraform modules:

    ```sh
    terraform init
    ```

    Get an infrastructure setup plan:

    ```sh
    terraform plan
    ```

    If you see the following output on a fresh project:

    ```terraform
    ...
    Plan: 193 to add, 0 to change, 0 to destroy.
    ```

    you can continue to apply the changes (needs confirmation after the planning step)

    ```sh
    terraform apply
    ```

    If your see the following output, your setup was successful:

    ```terraform
    ...
    Apply complete! Resources: 190 added, 0 changed, 0 destroyed.

    Outputs:
    create_job_endpoint = "POST https://<frontend_api_id>.execute-api.<aws_region>.amazonaws.com/stage/v1alpha/createJob"
    frontend_api_id = "xyz"
    get_job_endpoint = "GET https://<frontend_api_id>.execute-api.<aws_region>.amazonaws.com/stage/v1alpha/getJob"
    ```

    The Terraform scripts create `createJob` and `getJob` API endpoints:

    -   Create Job Endpoint:
        `https://<frontend_api_id>.execute-api.<aws_region>.amazonaws.com/stage/v1alpha/createJob`
    -   Get Job Endpoint:
        `https://<frontend_api_id>.execute-api.<aws_region>.amazonaws.com/stage/v1alpha/getJob`

    These are authenticated endpoints, refer to the [Testing the System](#testing-the-system)
    section to learn how to use them.

    _If you run into any issues during deployment of your system, please consult the
    [Troubleshooting](#troubleshooting) and [Support](/README.md#support) sections._

## Testing the system

To test the system, you'll need encrypted aggregatable reports in Avro batch format (follow the
[collecting and batching instructions](/docs/collecting.md#collecting-and-batching-aggregatable-reports)
accessible by the aggregation service.

If your inputs are larger than a few hundred MB, we suggest sharding the input reports and domain
file into smaller shards.

1. Create an S3 bucket for your input and output data, we will refer to it as `data_bucket`. This
   bucket must be created in the same AWS account where you set up the aggregation service. \*
   Consider enabling `versioning` to preserve, retrieve, and restore previous versions and set
   appropriate policies for this bucket to prevent accidental changes and deletion.

1. Copy your reports.avro with batched encrypted aggregatable reports to `<data_bucket>/input`.

1. Create an aggregation job with the `createJob` API.

    `POST` `https://<frontend_api_id>.execute-api.us-east-1.amazonaws.com/stage/v1alpha/createJob`

    ```json
    {
        "input_data_blob_prefix": "input/reports.avro",
        "input_data_bucket_name": "<data_bucket>",
        "output_data_blob_prefix": "output/summary_report.avro",
        "output_data_bucket_name": "<data_bucket>",
        "job_parameters": {
            "attribution_report_to": "<your_attribution_domain>",
            "output_domain_blob_prefix": "domain/domain.avro",
            "output_domain_bucket_name": "<data_bucket>"
        },
        "job_request_id": "test01"
    }
    ```

    _Note: This API requires authentication. Follow the
    [AWS instructions](https://aws.amazon.com/premiumsupport/knowledge-center/iam-authentication-api-gateway/)
    for sending an authenticated request._

1. Check the status of your job with the `getJob` API, replace values in `<...>`

    `GET`
    `https://<frontend_api_id>.execute-api.<deployment_aws_region>.amazonaws.com/stage/v1alpha/getJob?job_request_id=test01`

    _Note: This API requires authentication. Follow the
    [AWS instructions](https://aws.amazon.com/premiumsupport/knowledge-center/iam-authentication-api-gateway/)
    for sending an authenticated request. [Detailed API spec](/docs/api.md#getjob-endpoint)_

## Updating the system

Run the following in the `<repository_root>`.

```sh
git fetch origin && git checkout -b dev-v{VERSION} v{VERSION}
cd terraform/aws
bash download_prebuilt_dependencies.sh
cd environments/dev
terraform apply
```

_Note: If you use self-built artifacts described in
[build-scripts/aws](/build-scripts/aws/README.md), run `bash fetch_terraform.sh` instead of
`bash download_prebuilt_dependencies.sh` and make sure you updated your dependencies in the `jars`
folder._

## Troubleshooting

-   The following error message points to a potential lack of instance availability. If you
    encounter this situation, run `terraform destroy` to remove your deployment and run
    `terraform apply` again.

    ```txt
    Error: Error creating Auto Scaling Group: ValidationError: You must use a valid
    fully-formed launch template. Your requested instance type (m5.2xlarge) is not
    supported in your requested Availability Zone (us-east-1e).
    Please retry your request by not specifying an Availability Zone or choosing
    us-east-1a, us-east-1b, us-east-1c, us-east-1d, us-east-1f.
    ```

-   The following error message points to a potential lack of sufficient elastic VPC IPs quota in
    your deployment region. Request a
    [quota increase](https://docs.aws.amazon.com/vpc/latest/userguide/amazon-vpc-limits.html) or
    decrease the number of subnets created for the aggregation service VPC to resolve the issue. To
    stay within the default quota you can decrease the number of subnets, by setting
    `vpc_availability_zones = ["a","b","c","d","e"]` in your `<name>.auto.tfvars`.

    ```txt
    Error: Error creating EIP: AddressLimitExceeded: The maximum number of addresses has been reached.
        status code: 400, request id: 2c7a924c-c807-4714-8d77-8558a463c68b

        with module.operator_service.module.vpc[0].aws_eip.elastic_ip["us-east-1a"],
        on ../../modules/vpc/main.tf line 277, in resource "aws_eip" "elastic_ip":
        277: resource "aws_eip" "elastic_ip" {
    ```
