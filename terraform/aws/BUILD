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
load("@rules_pkg//:mappings.bzl", "pkg_files")
load("@rules_pkg//:pkg.bzl", "pkg_tar")
load("//build_defs:release.bzl", "s3_jar_release")
load(":build_defs.bzl", "ami_params_tfvar_file")

package(default_visibility = ["//visibility:public"])

string_flag(
    name = "bucket_flag",
    build_setting_default = "aggregation-service-published-artifacts",
    visibility = ["//visibility:public"],
)

string_flag(
    name = "bucket_path_flag",
    build_setting_default = "aggregation-service",
    visibility = ["//visibility:public"],
)

string_flag(
    name = "licenses_flag",
    build_setting_default = "aggregation-service",
    visibility = ["//visibility:public"],
)

# Shared ami_params.auto.tfvars value which contains the values configured the build flags.
ami_params_tfvar_file(
    name = "ami_params_auto_tfvars",
    file_name = "ami_params.auto.tfvars",
)

pkg_files(
    name = "dist_jars",
    srcs = [
        "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:AsgCapacityHandlerLambdaDeploy",
        "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:TerminatedInstanceHandlerLambdaDeploy",
        "//java/com/google/aggregate/adtech/worker/frontend:AwsApiGatewayFrontendDeployPackage",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws:AwsChangeHandlerLambdaDeploy",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws:AwsFrontendCleanupLambdaDeploy",
    ],
    prefix = "jars",
)

s3_jar_release(
    name = "aws_change_handler_lambda_release",
    artifact_base_name = "AwsChangeHandlerLambda_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker/frontend/service/aws:AwsChangeHandlerLambdaDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/aws_aws_change_handler_lambda_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "asg_capacity_handler_lambda_release",
    artifact_base_name = "AsgCapacityHandlerLambda_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:AsgCapacityHandlerLambdaDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/aws_asg_capacity_handler_lambda_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "terminated_instance_handler_lambda_release",
    artifact_base_name = "TerminatedInstanceHandlerLambda_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:TerminatedInstanceHandlerLambdaDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/aws_terminated_instance_handler_lambda_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "aws_api_gateway_frontend_handler_lambda_release",
    artifact_base_name = "AwsApiGatewayFrontend_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker/frontend:AwsApiGatewayFrontendDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/aws_aws_api_gateway_frontend_handler_lambda_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "aws_frontend_cleanup_handler_lambda_release",
    artifact_base_name = "AwsFrontendCleanupLambda_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker/frontend/service/aws:AwsFrontendCleanupLambdaDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/aws_aws_frontend_cleanup_handler_lambda_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "local_testing_tool_release",
    artifact_base_name = "LocalTestingTool_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/adtech/worker:LocalTestingToolDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/aws_local_testing_tool_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

s3_jar_release(
    name = "privacy_budget_unit_extraction_tool_release",
    artifact_base_name = "PrivacyBudgetUnitExtraction_{VERSION}.jar",
    jar_target = "//java/com/google/aggregate/tools/privacybudgetutil/aws:AwsPrivacyBudgetUnitExtractionDeploy",
    licenses = "//licenses/THIRD_PARTY_NOTICES/aws_privacy_budget_unit_extraction_tool_release:licenses_tar",
    release_bucket = ":bucket_flag",
    release_key = ":bucket_path_flag",
)

pkg_tar(
    name = "operator_tar",
    srcs = [
        ":dist_jars",
        "//terraform/aws/applications:applications_pkg",
        "//terraform/aws/environments/demo:demo_environment_pkg",
        "//terraform/aws/modules:modules_pkg",
        "@shared_libraries//licenses",
        "@shared_libraries//operator/aws/docs",
    ],
    extension = "tgz",
    strip_prefix = "/",
)
