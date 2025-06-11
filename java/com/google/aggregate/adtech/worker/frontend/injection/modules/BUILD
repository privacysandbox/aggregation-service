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
    name = "frontend_module",
    srcs = ["FrontendModule.java"],
    deps = [
        "modules",
        "//java/com/google/aggregate/adtech/worker/frontend/serialization",
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/converter",
        "//java/com/google/aggregate/adtech/worker/frontend/service/model",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules:data_module",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//protos/frontend/api/v1:java_proto",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//java/com/google/scp/shared/mapper",
    ],
)

java_library(
    name = "aws_change_handler_module",
    srcs = ["AwsChangeHandlerModule.java"],
    deps = [
        "modules",
        "//java/com/google/aggregate/adtech/worker/frontend/serialization",
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws/changehandler",
        "//java/com/google/aggregate/adtech/worker/frontend/service/converter",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/common",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules:aws_clients_module",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules:data_module",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:aws_apache_http",
        "//java/external:aws_dynamodb",
        "//java/external:aws_regions",
        "//java/external:aws_sqs",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//java/com/google/scp/shared/mapper",
    ],
)

java_library(
    name = "aws_failed_job_queue_change_handler_module",
    srcs = ["AwsFailedJobQueueChangeHandlerModule.java"],
    deps = [
        "modules",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws/changehandler",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
    ],
)

java_library(
    name = "aws_failed_job_queue_write_cleanup_module",
    srcs = ["AwsFailedJobQueueWriteCleanupModule.java"],
    deps = [
        "modules",
        "//java/com/google/aggregate/adtech/worker/frontend/serialization",
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws/changehandler",
        "//java/com/google/aggregate/adtech/worker/frontend/service/converter",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:aws_regions",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//java/com/google/scp/shared/mapper",
    ],
)

java_library(
    name = "modules",
    srcs = [
        "BaseAwsChangeHandlerModule.java",
        "BaseFrontendModule.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/serialization",
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws/changehandler",
        "//java/com/google/aggregate/adtech/worker/frontend/service/converter",
        "//java/com/google/aggregate/adtech/worker/frontend/service/model",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/injection/factories",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/frontend/api/v1:java_proto",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/model",
    ],
)
