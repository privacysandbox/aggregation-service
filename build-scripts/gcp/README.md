# Building aggregation service artifacts for Google Cloud

## Prerequisites

### Set up GCLOUD CLI

Make sure you [install](https://cloud.google.com/sdk/gcloud) and
[authenticate](https://cloud.google.com/sdk/docs/authorizing#auth-login) the latest gcloud CLI.

## Building the build container

The build container is requried for the aggregation service artifacts build.

To trigger the build, run the following command from the root of the repo:

```sh
gcloud builds submit --config=build-scripts/gcp/build-container/cloudbuild.yaml --substitutions=_IMAGE_REPO_PATH="<YourBuildContainerRegistryRepoPath>",_IMAGE_NAME="bazel-build-container",_IMAGE_TAG=$(cat VERSION)
```

The `<YourBuildContainerRegistryRepoPath>` is in the form of
`<location>-docker.pkg.dev/<project-id>/<artifact-repository-name>`.

The build can take several minutes. You can check the status at
`https://console.cloud.google.com/cloud-build/builds`.

## Building artifacts

To build the aggregation service artifacts the above build container is required. Make sure the
build for the build container ran successful before starting the build for the aggregation service
artifacts.

To trigger the artifacts build, run the following command from the root of the repo:

```sh
gcloud builds submit --config=build-scripts/gcp/cloudbuild.yaml --substitutions=_BUILD_IMAGE_REPO_PATH="<YourBuildContainerRegistryRepoPath>",_IMAGE_REPO_PATH="<YourOutputContainerRegistryRepoPath>",_IMAGE_NAME="worker_mp_gcp_prod",_IMAGE_TAG=$(cat VERSION),_JARS_PUBLISH_BUCKET="<YourArtifactsOutputBucketName>",_JARS_PUBLISH_BUCKET_PATH="aggregation-service",_VERSION=$(cat VERSION)
```

The build can take several minutes. You can check the status at
`https://console.cloud.google.com/cloud-build/builds`.

## Download artifacts

To download the artifacts you can use the `gsutil` command. Download the artifacts to
`<repository_root>/terraform/gcp/jars`. Run the following in `<repository_root>/terraform/gcp`

```sh
mkdir -p jars
gsutil cp -r gs://<YourArtifactsOutputBucketName>/aggregation-service/$(cat ../../VERSION)/ jars/
```

### Fetch Aggregation Service Terraform

Switch to `<repository_root>/terraform/gcp`.

Run `bash fetch_terraform.sh`.

After downloading the artifacts and running above script continue with
[Set up your deployment environment](/docs/gcp-aggregation-service.md#set-up-your-deployment-environment)
