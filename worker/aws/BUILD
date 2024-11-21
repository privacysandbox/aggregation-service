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

load("@bazel_skylib//rules:common_settings.bzl", "string_flag")
load("@rules_pkg//:pkg.bzl", "pkg_tar")
load("@shared_libraries//operator/worker/aws/build_defs:deploy.bzl", "worker_aws_deployment")

package(default_visibility = ["//visibility:public"])

exports_files([
    "setup_enclave.sh",
    "aggregation_worker_ami.pkr.hcl",
    "allocator.yaml",
])

# Ensure libnsm ends up in the load path.
pkg_tar(
    name = "libnsm-tar",
    srcs = [
        "@shared_libraries//build_defs/aws/kmstool:libnsm.so",
    ],
    mode = "0644",
    package_dir = "/lib/x86_64-linux-gnu/",
    visibility = [":__subpackages__"],
)

pkg_tar(
    name = "all_licenses",
    deps = [
        "//licenses:licenses_tar",
        "@shared_libraries//licenses:licenses_tar",
    ],
)

TEE_REPO_NAME = "@shared_libraries"

DEFAULT_ENCLAVE_ARGS = [
    "--client_config_env",
    "AWS",
    "--job_client",
    "AWS",
    "--result_logger",
    "LOCAL_TO_CLOUD",
    # coord-staging environment
    "--result_working_directory_path",
    "/",
    "--decryption_key_service",
    "AWS_ENCLAVE_CLI_MULTI_PARTY_DECRYPTION_KEY_SERVICE",
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

string_flag(
    name = "ami_name_flag",
    build_setting_default = "aggregate-service-enclave",
    visibility = [":__subpackages__"],
)

string_flag(
    name = "ami_owners_flag",
    build_setting_default = '["self"]',
    visibility = [":__subpackages__"],
)

string_flag(
    name = "aws_region_flag",
    build_setting_default = "us-east-1",
)

string_flag(
    name = "subnet_id_flag",
    build_setting_default = "",
)

PROD_ENCLAVE_ARGS = [
    "--client_config_env",
    "AWS",
    "--job_client",
    "AWS",
    "--result_logger",
    "LOCAL_TO_CLOUD",
    "--decryption_key_service",
    "AWS_ENCLAVE_CLI_MULTI_PARTY_DECRYPTION_KEY_SERVICE",
    "--trusted_party_region_override",
    "us-east-1",
    "--param_client",
    "AWS",  # getting parameters from AWS parameter store
    "--lifecycle_client",
    "AWS",
    "--metric_client",
    "AWS",
    "--noising",
    "DP_NOISING",
    "--return_stack_trace",
    "--parallel_summary_upload_enabled",
    "--streaming_output_domain_processing_enabled",
    "--parallel_fact_noising_enabled",
    "--labeled_privacy_budget_keys_enabled",
]
# Check the documentation for the worker_aws_deployment macro for details on
# what is available in this package.

# Target to generate AMI for multi-party coordinators.
worker_aws_deployment(
    name = "worker_mp_aws_prod",
    ami_name = ":ami_name_flag",
    aws_region = ":aws_region_flag",
    jar_args = PROD_ENCLAVE_ARGS + [
        # Coordinators regions
        "--coordinator_a_region_override",
        "us-east-1",
        "--coordinator_b_region_override",
        "us-east-1",

        # Key service related arguments
        "--decryption_key_service",
        "AWS_ENCLAVE_CLI_MULTI_PARTY_DECRYPTION_KEY_SERVICE",
        "--coordinator_a_encryption_key_service_base_url",
        "https://privatekeyservice-a.msmt-1.aws.privacysandboxservices.com/v1alpha",
        "--coordinator_b_encryption_key_service_base_url",
        "https://privatekeyservice-b.msmt-2.aws.privacysandboxservices.com/v1alpha",

        # PBS related arguments
        "--privacy_budgeting",
        "HTTP",

        # PBS endpoints
        "--coordinator_a_privacy_budgeting_endpoint",
        "https://mp-pbs-a.msmt-1.aws.privacysandboxservices.com/v1",
        "--coordinator_b_privacy_budgeting_endpoint",
        "https://mp-pbs-b.msmt-2.aws.privacysandboxservices.com/v1",

        # PBS auth endpoints
        "--coordinator_a_privacy_budget_service_auth_endpoint",
        "https://auth.mp-pbs-a.msmt-1.aws.privacysandboxservices.com/auth",
        "--coordinator_b_privacy_budget_service_auth_endpoint",
        "https://auth.mp-pbs-b.msmt-2.aws.privacysandboxservices.com/auth",
    ],
    jar_file = "/WorkerRunner_prod_deploy.jar",
    jvm_options = DEFAULT_JVM_OPTIONS,
    licenses = ":all_licenses",
    repo_name = TEE_REPO_NAME,
    subnet_id = ":subnet_id_flag",
    user_rpms = ["//worker/aws/telemetry/otel_collector:otel_collector_rpm"],
    worker_path = "//java/com/google/aggregate/adtech/worker:WorkerRunnerDeploy",
)
