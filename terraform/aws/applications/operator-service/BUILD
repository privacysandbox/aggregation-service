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

package(default_visibility = ["//visibility:public"])

pkg_files(
    name = "operator-service",
    srcs = glob(
        # Omit template and bzl files from generated tar.
        ["*.tf"],
    ),
    prefix = "operator-service",
)

alias(
    name = "frontendLambdaDeployable",
    actual = "//java/com/google/aggregate/adtech/worker/frontend:AwsApiGatewayFrontendDeploy",
)

alias(
    name = "changeHandlerLambdaDeployable",
    actual = "//java/com/google/aggregate/adtech/worker/frontend/service/aws:AwsChangeHandlerLambdaDeploy",
)

alias(
    name = "sqsWriteCleanupLambdaDeployable",
    actual = "//java/com/google/aggregate/adtech/worker/frontend/service/aws:AwsFrontendCleanupLambdaDeploy",
)

alias(
    name = "asgCapacityLambdaDeployable",
    actual = "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:AsgCapacityHandlerLambdaDeploy",
)

alias(
    name = "terminatedInstanceLambdaDeployable",
    actual = "//java/com/google/aggregate/adtech/worker/autoscaling/app/aws:TerminatedInstanceHandlerLambdaDeploy",
)

string_flag(
    name = "ami_name_flag",
    build_setting_default = "scp-enclave-fast-staging",
)

string_flag(
    name = "ami_owners_flag",
    build_setting_default = '["self"]',
)

exports_files([
    "env.template",
    "env-test.template",
])
