# Set up Aggregation Service for aggregatable reports

**[NOTE] The latest aggregatable reports generated with Chrome version 104+ are only supported with
version `0.3.0` and later. Follow the [update instructions](#updating-the-system) for your
environment.**

This repository contains instructions and scripts to set up and test the Aggregation Service for
aggregatable reports locally and on Amazon Web Services
[Nitro Enclaves](https://aws.amazon.com/ec2/nitro/nitro-enclaves/).

Learn more about the [Privacy Sandbox](https://privacysandbox.com/), the [Aggregation Service](https://developer.chrome.com/docs/privacy-sandbox/aggregation-service/) for Attribution Reporting and the Private Aggregation API, and [summary reports](https://developer.chrome.com/docs/privacy-sandbox/summary-reports/).

## Set up local testing

You can process
[aggregatable debug reports](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports)
locally with the `LocalTestingTool_{VERSION}.jar` into summary reports. Learn about Attribution Reporting
[debug reports](https://developer.chrome.com/docs/privacy-sandbox/attribution-reporting-debugging/).

_Disclaimer: encrypted reports can **not** be processed with the local testing tool!_

### Clone the repository

Clone the repository into a local folder `<repostory_root>`:

```sh
git clone https://github.com/privacysandbox/aggregation-service;
cd aggregation-service
```

### Using the local testing tool

The local testing tool can be used to perform aggregation on the following types of unencrypted
aggregatable reports:

1. [Attribution Reporting](https://github.com/WICG/conversion-measurement-api/blob/main/AGGREGATE.md#aggregatable-reports)
2. [FLEDGE](https://github.com/patcg-individual-drafts/private-aggregation-api#reports) with the Private Aggregation API
3. [Shared Storage](https://github.com/patcg-individual-drafts/private-aggregation-api#reports) with the Private Aggregation API

You can pass any of the above reports as `--input_data_avro_file` param.

Download the local testing tool by running the following command in the `<repository_root>`:

```sh
VERSION=$(cat VERSION); curl -f -o LocalTestingTool_$VERSION.jar https://aggregation-service-published-artifacts.s3.amazonaws.com/aggregation-service/$VERSION/LocalTestingTool_$VERSION.jar
```

You'll need [Java JRE](https://adoptium.net/) installed to use the tool.

<!-- prettier-ignore-start -->
_The `SHA256` of the `LocalTestingTool_{version}.jar` can be found on the
[releases page](https://github.com/privacysandbox/aggregation-service/releases)._
<!-- prettier-ignore-end -->

Follow the instructions on how to
[collect and batch aggregatable reports](#collect-and-batch-aggregatable-reports). Create an output
domain file: `output_domain.avro`. For testing you can use our
[sample debug batch](./sampledata/output_debug_reports.avro) with the corresponding
[output domain avro](./sampledata/output_domain.avro).

To aggregate the resulting avro batch `output_debug_reports.avro` file into a summary report in the
same directory where you run the tool, run the following command:

```sh
java -jar LocalTestingTool_{version}.jar \
--input_data_avro_file output_debug_reports.avro \
--domain_avro_file output_domain.avro \
--output_directory .
```

To see all supported flags for the local testing tool run
`java -jar LocalTestingTool_{version}.jar --help`, e.g. you can adjust the noising epsilon with the
`--epsilon` flag or disable noising all together with the `--no_noising` flag.
[See all flags and descriptions](/docs/API.md#local-testing-tool).

## Test on AWS with support for encrypted reports

### No duplicate reports

Aggregation Service enforces the
[no-duplicates](https://github.com/WICG/attribution-reporting-api/blob/main/AGGREGATION_SERVICE_TEE.md#no-duplicates-rule)
rule. We recommend users design their systems keeping the no-duplicate rule in consideration.
We suggest reading the [debugging](/docs/DEBUGGING.md) document for debug aggregation runs.

### Prerequisites

To test the Aggregation Service with support for encrypted reports, you need the following:

-   Have an [AWS account](https://portal.aws.amazon.com/gp/aws/developer/registration/index.html)
    available to you.
-   [Register](https://developer.chrome.com/origintrials/#/view_trial/771241436187197441) for the
    Privacy Sandbox Relevance and Measurement origin trial (OT)
-   Complete the Aggregation Service [onboarding form](https://forms.gle/EHoecersGKhpcLPNA)

Once you've submitted the onboarding form, we will contact you to verify your information. Then,
we'll send you the remaining instructions and information needed for this setup.</br> _You won't be
able to successfully setup your AWS system without registering for the origin trial and completing
the onboarding process!_

To set up Aggregation Service in AWS you'll use [Terraform](https://www.terraform.io/).

### Set up AWS client

Make sure you
[install](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) and
[set up](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html) the latest
AWS client.

### Set up Terraform

Change into the `<repository_root>/terraform/aws` folder. See
[clone the repository](#clone-the-repository) if you have not cloned the repository so far.

The setup scripts require terraform version `1.2.3`. You can download Terraform version 1.2.3 from
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

### Download Terraform scripts and prebuilt dependencies

_Note: The prebuilt Amazon Machine Image (AMI) for the Aggregation Service is only available in the
`us-east-1` region. If you like to deploy the Aggregation Service in a different region you need to
copy the released AMI to your account or build it using our provided scripts._

If you like to build the Amazon Machine Image including the enclave container, as well as the Lambda
jars in your account, follow the instructions in
[build-scripts/aws](/build-scripts/aws/README.md). This will skip running
`bash download_prebuilt_dependencies.sh` and run `bash fetch_terraform.sh` instead. Continue with
the [next deployment step](#set-up-your-deployment-environment) after building and downloading your
self-build jars.

The Terraform scripts to deploy the Aggregation Service depend on 5 packaged jars for Lambda
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

### Set up your deployment environment

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

1.  **Skip this step if you use our prebuilt AMI and Lambda jars**

    If you [self-build your AMI and jars](/build-scripts/aws/README.md), you need to copy the
    contents of the `release_params.auto.tfvars` file into a new file
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
    Plan: 190 to add, 0 to change, 0 to destroy.
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

    The terraform scripts create `createJob` and `getJob` API endpoints:

    -   Create Job Endpoint:
        `https://<frontend_api_id>.execute-api.<aws_region>.amazonaws.com/stage/v1alpha/createJob`
    -   Get Job Endpoint:
        `https://<frontend_api_id>.execute-api.<aws_region>.amazonaws.com/stage/v1alpha/getJob`

    These are authenticated endpoints, refer to the [Testing the System](#testing-the-system)
    section to learn how to use them.

    _If you run into any issues during deployment of your system, consult the
    [Troubleshooting](#troubleshooting) and [Support](#support) sections._

### Testing the system

To test the system, you'll need encrypted aggregatable reports in avro batch format (follow the
[collecting and batching instructions](#collect-and-batch-aggregatable-reports)) accessible by the
aggregation service.

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
    for sending an authenticated request. [Detailed API spec](/docs/API.md#getjob-endpoint)_

### Updating the system

If you have deployed the system before, we recommend to run `terraform destroy` in your environment
folder (e.g. `<repository_root>/terraform/aws/environments/dev`) when upgrading from `0.3.z` to
`0.4.z+` and follow the [setup steps](#set-up-your-deployment-environment) again.

After your upgrade to `0.4.z+` and if you have followed the above setup, next time you can update
your system to the latest version by checking out the latest tagged version and running
`terraform apply` in your environment folder (e.g.
`<repository_root>/terraform/aws/environments/dev`).

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

## Collect and batch aggregatable reports

Both the local testing tool and the aggregation service running on AWS Nitro Enclave expect
aggregatable reports batched in the following [Avro](https://avro.apache.org/) format.

```avro
{
  "type": "record",
  "name": "AggregatableReport",
  "fields": [
    {
      "name": "payload",
      "type": "bytes"
    },
    {
      "name": "key_id",
      "type": "string"
    },
    {
      "name": "shared_info",
      "type": "string"
    }
  ]
}
```

Additionally an output domain file is needed to declare all expected aggregation keys for
aggregating the aggregatable reports (keys not listed in the domain file won't be aggregated)

```avro
{
 "type": "record",
 "name": "AggregationBucket",
 "fields": [
   {
     "name": "bucket",
     "type": "bytes"
     /* A single bucket that appears in
     the aggregation service output.
     128-bit integer encoded as a
     16-byte big-endian byte string. */
   }
 ]
}
```

[Review code snippets](/docs/COLLECTING.md) which demonstrate how to collect and batch aggregatable
reports.

## Generate debug summary reports

Refer to [Debug aggregation runs](/docs/DEBUGGING.md) and
[Debugging the Attribution Reporting API](https://developer.chrome.com/docs/privacy-sandbox/attribution-reporting-debugging/)
for more details about debugging support in Aggregation Service.

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

## Support

You can reach out to us for support through creating issues on this repository or sending us an
email at aggregation-service-support\<at>google.com. This address is monitored and only visible to
selected support staff.

## License

Apache 2.0 - See [LICENSE](LICENSE) for more information.

## FAQ

### Where should I post feedback/questions, this repo or the Attribution API repo?

This repo hosts an implementation of the
[Attribution Reporting API](https://github.com/WICG/attribution-reporting-api). For
feedback/questions encountered during using this particular aggregation service implementation,
use the support channels provided by this repo.

For feedback/requests related to the Privacy Sandbox APIs, ask questions and join discussions
on the [Privacy Sandbox Developer Support repo](https://github.com/GoogleChromeLabs/privacy-sandbox-dev-support). 
