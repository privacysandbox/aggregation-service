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
    name = "aws",
    srcs = glob([
        "*.java",
        "aws/*.java",
    ]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/common",
        "//java/external:aws_apache_http",
        "//java/external:aws_auth",
        "//java/external:aws_auto_scaling",
        "//java/external:aws_core",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:aws_ec2",
        "//java/external:aws_http_client_spi",
        "//java/external:aws_regions",
        "//java/external:aws_ssm",
        "//java/external:aws_sts",
        "//java/external:aws_utils",
        "//java/external:findbugs",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:slf4j",
        "//protos/shared/backend/asginstance:java_proto",
        "@shared_libraries//java/com/google/scp/operator/cpio/configclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/configclient:aws",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:model",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)

java_library(
    name = "gcp",
    srcs = glob([
        "*.java",
        "gcp/*.java",
    ]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/gcp",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:findbugs",
        "//java/external:gcp_api_gax",
        "//java/external:gcp_compute",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:proto_gcp_compute_v1",
        "//java/external:slf4j",
        "//java/external:slf4j_simple",
        "//protos/shared/backend/asginstance:java_proto",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:gcp",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:model",
    ],
)

java_library(
    name = "local",
    srcs = glob([
        "*.java",
        "local/*.java",
    ]),
    deps = [
        "//java/external:guice",
    ],
)

java_library(
    name = "lifecycleclient",
    srcs = glob([
        "*.java",
    ]),
    deps = [
        "//java/external:guice",
    ],
)
