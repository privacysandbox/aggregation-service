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
    name = "jobclient",
    srcs = glob(["*.java"]),
    deps = [
        ":model",
        "//java/com/google/aggregate/adtech/worker/lifecycleclient",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:findbugs",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:protobuf_java_util",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/jobqueue:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//java/com/google/scp/operator/cpio/metricclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/metricclient:model",
        "@shared_libraries//java/com/google/scp/operator/cpio/notificationclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/notificationclient:model",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:model",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)

java_library(
    name = "model",
    srcs = glob(["model/*.java"]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:guava",
        "//java/external:jackson_annotations",
        "//protos/shared/backend:java_proto",
    ],
)

java_library(
    name = "aws",
    srcs = glob([
        "*.java",
        "local/*.java",
        "aws/*.java",
        "testing/*.java",
    ]),
    deps = [
        ":model",
        "//java/com/google/aggregate/adtech/worker/lifecycleclient",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:job_generator",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:aws_apache_http",
        "//java/external:aws_auth",
        "//java/external:aws_core",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:aws_regions",
        "//java/external:aws_sqs",
        "//java/external:findbugs",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:javax_inject",
        "//java/external:protobuf_java_util",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/jobqueue:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@com_google_protobuf//:protobuf_java",
        "@shared_libraries//java/com/google/scp/operator/cpio/configclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/metricclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/metricclient:model",
        "@shared_libraries//java/com/google/scp/operator/cpio/notificationclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/notificationclient:model",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:aws",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:model",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)

java_library(
    name = "gcp",
    srcs = glob([
        "*.java",
        "local/*.java",
        "gcp/*.java",
        "testing/*.java",
    ]),
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/lifecycleclient",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/gcp",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/gcp",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:job_generator",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:findbugs",
        "//java/external:gcp_api_gax",
        "//java/external:gcp_pubsub",
        "//java/external:gcp_pubsub_v1",
        "//java/external:gcp_spanner",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:javax_inject",
        "//java/external:protobuf_java_util",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/jobqueue:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient:model",
        "@shared_libraries//java/com/google/scp/operator/cpio/configclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/metricclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/metricclient:model",
        "@shared_libraries//java/com/google/scp/operator/cpio/notificationclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/notificationclient:model",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:gcp",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:model",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)
