# Copyright 2025 Google LLC
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

load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_pkg//:mappings.bzl", "pkg_files")

package(default_visibility = ["//visibility:public"])

java_binary(
    name = "AwsApigatewayFrontend",
    resources = [
        "//licenses/THIRD_PARTY_NOTICES/aws_aws_api_gateway_frontend_handler_lambda_release:licenses",
    ],
    runtime_deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/injection/modules:frontend_module",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws",
        "//java/external:commons_logging",
    ],
)

alias(
    name = "AwsApiGatewayFrontendDeploy",
    actual = ":AwsApigatewayFrontend_deploy.jar",
)

pkg_files(
    name = "AwsApiGatewayFrontendDeployPackage",
    srcs = [
        ":AwsApiGatewayFrontendDeploy",
    ],
    prefix = "jars",
    strip_prefix = ".",
)
