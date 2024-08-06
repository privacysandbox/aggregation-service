# Testing on GCP using encrypted reports

## Prerequisites

To test the aggregation service with support for encrypted reports, you need the following:

-   Have a [GCP project](https://cloud.google.com/).
-   Run the [Adtech Setup Terraform](#adtech-setup-terraform) to create/configure the service
    account needed for onboarding.
-   Complete the aggregation service [onboarding form](https://forms.gle/EHoecersGKhpcLPNA)

Once you've submitted the onboarding form, we will contact you to verify your information. Then,
we'll send you the remaining instructions and information needed for this setup.</br> _You won't be
able to successfully setup your GCP deployment without completing the onboarding process!_

To set up aggregation service in GCP you'll use [Terraform](https://www.terraform.io/).

## Set up GCLOUD CLI

Make sure you [install](https://cloud.google.com/sdk/gcloud) and
[authenticate](https://cloud.google.com/sdk/docs/authorizing#auth-login) the latest gcloud CLI.

## Set up Terraform

Change into the `<repository_root>/terraform/gcp` folder. See
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
bucket. Create a Google Cloud Storage bucket via the console/cli, which we'll reference as
`tf_state_bucket_name`. Consider enabling `versioning` to preserve, retrieve, and restore previous
versions and set appropriate policies for this bucket to prevent accidental changes and deletion.

```sh
gsutil mb gs://<tf_state_bucket_name>
```

[Authenticate](https://cloud.google.com/sdk/docs/authorizing#adc) gcloud cli for terraform.

```sh
gcloud auth application-default login
```

## Download Terraform scripts and prebuilt dependencies

If you like to build the Confidential Space container, as well as the Cloud Function jars in your
account, please follow the instructions in [build-scripts/gcp](/build-scripts/gcp/README.md). Skip
running `bash download_prebuilt_dependencies.sh` and run `bash fetch_terraform.sh` instead. Continue
with the [next deployment step](#set-up-your-deployment-environment) after building and downloading
your self-build jars.

The Terraform scripts to deploy the aggregation service depend on 2 packaged jars for Cloud
functions deployment. These jars are hosted on Google Cloud Storage
(<https://storage.googleapis.com/aggregation-service-published-artifacts/aggregation-service/{version}/{jar_file>})
and can be downloaded with the `<repository_root>/terraform/gcp/download_prebuilt_dependencies.sh`
script. The script downloads the terrafrom scripts and jars which will be stored in
`<repository_root>/terraform/gcp`. License information of downloaded dependencies can be found in
the [DEPENDENCIES.md](/DEPENDENCIES.md)

Run the following script in the `<repository_root>/terraform/gcp` folder to download the prebuilt
dependencies.

```bash
bash download_prebuilt_dependencies.sh
```

_Note: The above script needs to be run with `bash` and does not support `sh`\*_

For manual download into the `<repository_root>/terraform/gcp/jars` folder you can download them
from the links on our
[releases page](https://github.com/privacysandbox/aggregation-service/releases).

## Adtech Setup Terraform

Make sure you have completed the steps above before following the next instructions.

-   [Set up GCLOUD CLI](#set-up-gcloud-cli)
-   [Set up Terraform](#set-up-terraform)
-   [Download Terraform scripts and prebuilt dependencies](#download-terraform-scripts-and-prebuilt-dependencies)

Make the following adjustments in the `<repository_root>/terraform/gcp/environments/adtech_setup`
folder:

1.  Copy `main.tf_sample` to `main.tf` and add the `tf_state_bucket_name` to your `main.tf` by
    uncommenting and replacing the values using `<...>`:

    ```sh
    # backend "gcs" {
    #   bucket = "<tf_state_bucket_name>"
    #   prefix = "adtech_setup-tfstate"
    # }
    ```

1.  Copy `adtech_setup.auto.tfvars_sample` to `adtech_setup.auto.tfvars` and replace the values
    using `<...>` following instructions in the file.

1.  Once you've adjusted the configuration, run the following in the
    `<repository_root>/terraform/gcp/environments/adtech_setup` folder

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
    Plan: ?? to add, 0 to change, 0 to destroy.
    ```

    you can continue to apply the changes (needs confirmation after the planning step)

    ```sh
    terraform apply
    ```

    If your see the following output, your setup was successful:

    ```terraform
    ...
    Apply complete! Resources: 54 added, 0 changed, 0 destroyed.
    ...
    ```

_Note: Please be advised that executing `terraform destroy` for the Adtech Setup environment will
result in the deletion of all resources generated within that environment._

## Set up your deployment environment

_Note: Please, make sure that you have completed the above [Prerequisites](#prerequisites),
including the onboarding process._

We use the following folder structure
`<repository_root>/terraform/gcp/environments/<environment_name>` to separate deployment
environments.

To set up your first environment (e.g `dev`), copy the `demo` environment. Run the following
commands from the `<repository_root>/terraform/gcp/environments` folder:

```sh
mkdir dev
cp -R demo/* dev
cd dev
```

Make the following adjustments in the `<repository_root>/terraform/gcp/environments/dev` folder:

1.  Add the `tf_state_bucket_name` to your `main.tf` by uncommenting and replacing the values using
    `<...>`:

    ```sh
    # backend "gcs" {
    #   bucket = "<tf_state_bucket_name>"
    #   prefix    = "<environment_name>-tfstate"
    # }
    ```

1.  Rename `example.auto.tfvars` to `<environment>.auto.tfvars` and replace the values using
    `<...>`. Leave all other values as-is for the initial deployment.

    ```sh
    project_id  = "<YourProjectID>"
    environment = "<environment_name>"
    ...
    alarms_enabled           = true
    alarm_notification_email = "<noreply@example.com>"
    ```

    -   project_id: Google Cloud project ID for your deployment
    -   environment: name of your environment
    -   user_provided_worker_sa_email: Set to worker service account created in
        [Adtech Setup section](./docs/gcp-aggregation-service.md#adtech-setup-terraform) and
        submitted in [onboarding form](./docs/gcp-aggregation-service.md#prerequisites)
    -   alarm_enabled: boolean flag to enable alarms (default: false)
    -   alarm_notification_email: Email to receive alarm notifications.

1.  **Skip this step if you use our prebuilt container image and Cloud Function jars**

    If you [self-build your container image and jars](/build-scripts/gcp/README.md), you need to
    copy the contents of the `release_params.auto.tfvars` file into a new file
    `self_build_params.auto.tfvars` remove the `release_params.auto.tfvars` file afterwards.

    To copy without symlink, run the following in the
    `<repository_root>/terraform/gcp/environments/dev` folder

    ```sh
    cp -L release_params.auto.tfvars self_build_params.auto.tfvars
    ```

    Then delete the symlinked file:

    ```sh
    rm release_params.auto.tfvars
    ```

    And change the line `worker_image` to your location of the self built container image.

1.  To run the aggregation service deployment with the **deploy service account** created in
    [Adtech Setup](#adtech-setup-terraform), set the following environment variable:

    ```sh
    export GOOGLE_IMPERSONATE_SERVICE_ACCOUNT="<YourDeployServiceAccountName>@<ProjectID>.iam.gserviceaccount.com"
    ```

1.  Once you've adjusted the configuration, run the following in the
    `<repository_root>/terraform/gcp/environments/dev` folder

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
    Plan: 54 to add, 0 to change, 0 to destroy.
    ```

    you can continue to apply the changes (needs confirmation after the planning step)

    ```sh
    terraform apply
    ```

    If your see the following output, your setup was successful:

    ```terraform
    ...
    Apply complete! Resources: 54 added, 0 changed, 0 destroyed.

    Outputs:
    frontend_service_cloudfunction_url = "https://<environment>-us-central1-frontend-service-<cloud-function-id>-uc.a.run.app"
    vpc_network = "https://www.googleapis.com/compute/v1/projects/<project>/global/networks/<environment>-network"
    ```

    The Terraform scripts create `createJob` and `getJob` API endpoints:

    -   Create Job Endpoint:
        `https://<environment>-<region>-frontend-service-<cloud-funtion-id>-uc.a.run.app/v1alpha/createJob`
    -   Get Job Endpoint:
        `https://<environment>-<region>-frontend-service-<cloud-funtion-id>-uc.a.run.app/v1alpha/getJob`

    These are authenticated endpoints, refer to the [Testing the System](#testing-the-system)
    section to learn how to use them.

    _If you run into any issues during deployment of your system, please consult the
    [Support](/README.md#support) section._

## Testing the system

To test the system, you'll need encrypted aggregatable reports in Avro batch format (follow the
[collecting and batching instructions](/docs/collecting.md#collecting-and-batching-aggregatable-reports)
accessible by the aggregation service.

If your inputs are larger than a few hundred MB, we suggest sharding the input reports and domain
file into smaller shards.

1. Create a Google Cloud Storage bucket for your input and output data (if not done during Adtech
   Setup), we will refer to it as `data_bucket`. This bucket should be created in the same Google
   Cloud project where you set up the aggregation service.

    _Consider enabling `versioning` to preserve, retrieve, and restore previous versions and set
    appropriate policies for this bucket to prevent accidental changes and deletion._

1. Copy your reports.avro with batched encrypted aggregatable reports to `<data_bucket>/input`.

1. Create an aggregation job with the `createJob` API.

    `POST`
    `https://<environment>-<region>-frontend-service-<cloud-funtion-id>-uc.a.run.app/v1alpha/createJob`

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
    [Google Cloud Function instructions](https://cloud.google.com/functions/docs/securing/authenticating)
    for sending an authenticated request._

1. Check the status of your job with the `getJob` API, replace values in `<...>`

    `GET`
    `https://<environment>-<region>-frontend-service-<cloud-funtion-id>-uc.a.run.app/v1alpha/getJob?job_request_id=test01`

    _Note: This API requires authentication. Follow the
    [Google Cloud Function instructions](https://cloud.google.com/functions/docs/securing/authenticating)
    for sending an authenticated request. [Detailed API spec](/docs/api.md#getjob-endpoint)_

## Updating the system

Run the following in the `<repository_root>`.

```sh
git fetch origin && git checkout -b dev-v{VERSION} v{VERSION}
cd terraform/gcp
bash download_prebuilt_dependencies.sh
cd environments/dev
terraform apply
```

_Note: If you use self-built artifacts described in
[build-scripts/gcp](/build-scripts/gcp/README.md), run `bash fetch_terraform.sh` instead of
`bash download_prebuilt_dependencies.sh` and make sure you updated your dependencies in the `jars`
folder._

_Note: When migrating to new coordinator pair from version 2.[4|5|6].z to 2.7.z or later, ensure the
file `/terraform/gcp/environments/shared/release_params.auto.tfvars` was updated with the following
values:_

```sh
coordinator_a_impersonate_service_account = "a-opallowedusr@ps-msmt-coord-prd-g3p-svcacc.iam.gserviceaccount.com"
coordinator_b_impersonate_service_account = "b-opallowedusr@ps-prod-msmt-type2-e541.iam.gserviceaccount.com"
```
