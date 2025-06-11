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

load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "modules",
    srcs = [
        "BaseAwsClientsModule.java",
        "BaseDataModule.java",
        "EnvironmentVariables.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws/model/converter",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/external:aws_core2",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:aws_kms",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:aws_pricing",
        "//java/external:aws_regions",
        "//java/external:aws_s3",
        "//java/external:aws_sqs",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_library(
    name = "data_module",
    srcs = [
        "DataModule.java",
    ],
    deps = [
        "modules",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/validation",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws/model/converter",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/environment",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules:aws_clients_module",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:aws_regions",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_library(
    name = "aws_clients_module",
    srcs = [
        "AwsClientsModule.java",
    ],
    deps = [
        "aws_clients_configurable_module",
        "modules",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/environment",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:aws_auth",
        "//java/external:aws_core2",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:aws_pricing",
        "//java/external:aws_regions",
        "//java/external:aws_s3",
        "//java/external:aws_sqs",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_library(
    name = "aws_clients_configurable_module",
    srcs = [
        "AwsClientsConfigurableModule.java",
    ],
    deps = [
        "modules",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/environment",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:aws_auth",
        "//java/external:aws_core2",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:aws_regions",
        "//java/external:aws_s3",
        "//java/external:aws_sqs",
        "//java/external:aws_url_connection_client",
        "//java/external:guava",
        "//java/external:guice",
    ],
)
