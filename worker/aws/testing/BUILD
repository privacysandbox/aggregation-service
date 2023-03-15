# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@com_google_adm_cloud_scp//operator/worker/aws/build_defs:deploy.bzl", "worker_aws_deployment")

TEE_REPO_NAME = "@com_google_adm_cloud_scp"

KOKORO_TESTING_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDEPdcM5+bB1+vfFBjtTQUnNIc85mSiTHYLk2GuBJAsFxCgAyMqF7jXigqZ/95c4pFiEbcyCd2WCyF0/qeqyeoxMzTylE8njAoK6KlrNKPHWh/PnZcTpjCpUZcv4qkkaLA1emUXleSMwixImJJAPrSA5gVd6EFNBPvOvhIpFDQI0CPKxQRqP7ML+lfT/CZy5rySaXBtWr/UkeOmeU7g/ja5E9PR4ecPlsLiDuqhyZpUcIxPEpA8G8Y7hrMOMLB+FsFhq/lIz2tOUl5sA/8XgVJTI79PQZDk28S9kLu2/6OBGIo4qXumlQPHi2ZQxK1YvvuC/60PrXpwQvsuEEkzdpiz"

DEFAULT_ENCLAVE_ARGS = [
    "--client_config_env",
    "AWS",
    "--job_client",
    "AWS",
    "--result_logger",
    "LOCAL_TO_CLOUD",
    # coord-staging environment
    "--private_key_service_base_url",
    "https://il9t0ajpsa.execute-api.us-west-2.amazonaws.com/stage/v1alpha",
    "--result_working_directory_path",
    "/",
    "--param_client",
    "AWS",  # getting parameters from AWS parameter store
    "--lifecycle_client",
    "AWS",
    "--metric_client",
    "AWS",
    "--simulation_inputs",  # Note: this should only be used in dev environments.
    "--noising",
    "CONSTANT_NOISING",
]

DEFAULT_JVM_OPTIONS = [
    # Setting max JVM heap size to 75% of total memory in the enclave.
    "-XX:MaxRAMPercentage=75",
    # Exit the JVM program when out of memory. Use `HeapDumpOnOutOfMemoryError`
    # JVM option to generate heap dump when out of memory.
    "-XX:+ExitOnOutOfMemoryError",
]

# Enclave args to be used by e2e tests during release verification process
RELEASE_ENCLAVE_ARGS = [
    "--client_config_env",
    "AWS",
    "--job_client",
    "AWS",
    "--result_logger",
    "LOCAL_TO_CLOUD",
    "--decryption_key_service",
    "AWS_ENCLAVE_CLI_DECRYPTION_KEY_SERVICE",
    "--param_client",
    "AWS",  # getting parameters from AWS parameter store
    "--lifecycle_client",
    "AWS",
    "--metric_client",
    "AWS",
    "--noising",
    "DP_NOISING",
]

# AMI for postsubmit e2e
worker_aws_deployment(
    name = "worker_aws_continuous",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    # Use noise parameters that match those provided when the golden output were generated
    jar_args = DEFAULT_ENCLAVE_ARGS + [
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_DECRYPTION_KEY_SERVICE",
        "--noising_epsilon",
        "0.1",
        "--noising_l1_sensitivity",
        "4",
        # Ignore privacy budgeting when using single coordinator
        "--privacy_budgeting",
        "UNLIMITED",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for postsubmit mp e2e
worker_aws_deployment(
    name = "worker_aws_continuous_mp",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    # Use noise parameters that match those provided when the golden output were generated
    jar_args = DEFAULT_ENCLAVE_ARGS + [
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_MULTI_PARTY_DECRYPTION_KEY_SERVICE",
        "--noising_epsilon",
        "0.1",
        "--noising_l1_sensitivity",
        "4",
        # Enforce privacy budgeting
        "--privacy_budgeting",
        "HTTP",
        "--coordinator_a_region_override",
        "us-east-1",
        "--coordinator_b_region_override",
        "us-east-1",
        # multi-party coordinator staging environment
        "--coordinator_a_encryption_key_service_base_url",
        "https://16gotxmx3i.execute-api.us-east-1.amazonaws.com/stage/v1alpha",
        "--coordinator_b_encryption_key_service_base_url",
        "https://4s55w7af1j.execute-api.us-east-1.amazonaws.com/stage/v1alpha",
        "--coordinator_a_privacy_budgeting_endpoint",
        "http://staging-mp-a-google-scp-pbs.eba-ciz8xs3x.us-east-1.elasticbeanstalk.com/v1",
        "--coordinator_a_privacy_budget_service_auth_endpoint",
        "https://k6519hbfl6.execute-api.us-east-1.amazonaws.com/staging-mp-a/auth",
        "--coordinator_b_privacy_budgeting_endpoint",
        "http://staging-mp-b-google-scp-pbs.eba-23eqaamr.us-east-1.elasticbeanstalk.com/v1",
        "--coordinator_b_privacy_budget_service_auth_endpoint",
        "https://uip0fu27i1.execute-api.us-east-1.amazonaws.com/staging-mp-b/auth",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for multi-party coordinator release e2e
worker_aws_deployment(
    name = "worker_aws_release_mp",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    # Use noise parameters that match those provided when the golden output were generated
    jar_args = DEFAULT_ENCLAVE_ARGS + [
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_MULTI_PARTY_DECRYPTION_KEY_SERVICE",
        "--noising_epsilon",
        "0.1",
        "--noising_l1_sensitivity",
        "4",
        # Enforce privacy budgeting
        "--privacy_budgeting",
        "HTTP",
        "--coordinator_a_region_override",
        "us-east-1",
        "--coordinator_b_region_override",
        "us-east-1",
        # multi-party coordinator staging environment
        "--coordinator_a_encryption_key_service_base_url",
        "https://8phnnmek2d.execute-api.us-east-1.amazonaws.com/stage/v1alpha",
        "--coordinator_b_encryption_key_service_base_url",
        "https://a8tht9f6gk.execute-api.us-east-1.amazonaws.com/stage/v1alpha",
        "--coordinator_a_privacy_budgeting_endpoint",
        "http://release-mp-a-google-scp-pbs.eba-f5bmbvft.us-east-1.elasticbeanstalk.com/v1",
        "--coordinator_a_privacy_budget_service_auth_endpoint",
        "https://bqmz51zd8b.execute-api.us-east-1.amazonaws.com/release-mp-a/auth",
        "--coordinator_b_privacy_budgeting_endpoint",
        "http://release-mp-b-google-scp-pbs.eba-paskivqt.us-east-1.elasticbeanstalk.com/v1",
        "--coordinator_b_privacy_budget_service_auth_endpoint",
        "https://1tb9veg40j.execute-api.us-east-1.amazonaws.com/release-mp-b/auth",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for release e2e
worker_aws_deployment(
    name = "worker_aws_e2e_release",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    jar_args = DEFAULT_ENCLAVE_ARGS + [
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_DECRYPTION_KEY_SERVICE",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for e2e test of coord-tar-release environment.
worker_aws_deployment(
    name = "worker_aws_e2e_coord_tar_release",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    jar_args = RELEASE_ENCLAVE_ARGS + [
        "--private_key_service_base_url",
        "https://t1neuq9z2c.execute-api.us-east-1.amazonaws.com/stage/v1alpha",
        "--trusted_party_region_override",
        "us-east-1",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for e2e adtech release
worker_aws_deployment(
    name = "worker_aws_e2e_adtech_release",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    jar_args = RELEASE_ENCLAVE_ARGS + [
        "--private_key_service_base_url",
        "https://privatekeyservice-prod-staging.aws-staging.admcstesting.dev/v1alpha",
        "--trusted_party_region_override",
        "us-east-1",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for e2e coordinator release.
worker_aws_deployment(
    name = "worker_aws_e2e_coordinator_release",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    # Private key url is from environment created using khs-coord-release environment
    # Privacy budget url is from environment created using pbs-coord-release environment
    jar_args = RELEASE_ENCLAVE_ARGS + [
        "--private_key_service_base_url",
        "https://kz64fnjxhi.execute-api.us-east-1.amazonaws.com/stage/v1alpha",
        "--trusted_party_region_override",
        "us-east-1",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for postsubmit perf tests
worker_aws_deployment(
    name = "worker_aws_continuous_perf",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    jar_args = DEFAULT_ENCLAVE_ARGS + [
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_DECRYPTION_KEY_SERVICE",
        "--stopwatch_bucket_name",
        "aggregation-service-testing",
        "--stopwatch_key_name",
        "stopwatches",
        "--timer_exporter",
        "AWS",
        "--benchmark",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for invalid test
worker_aws_deployment(
    name = "worker_aws_continuous_invalid_credentials",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    jar_args = DEFAULT_ENCLAVE_ARGS + [
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_DECRYPTION_KEY_SERVICE",
        "--stopwatch_bucket_name",
        "aggregation-service-testing",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)

# AMI for xcode e2e
worker_aws_deployment(
    name = "worker_aws_e2e_xcode",
    ami_name = "//worker/aws:ami_name_flag",
    aws_region = "//worker/aws:aws_region_flag",
    enable_worker_debug_mode = True,
    jar_args = DEFAULT_ENCLAVE_ARGS + [
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_DECRYPTION_KEY_SERVICE",
    ],
    jar_file = "/WorkerRunner_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    repo_name = TEE_REPO_NAME,
    subnet_id = "//worker/aws:subnet_id_flag",
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)
