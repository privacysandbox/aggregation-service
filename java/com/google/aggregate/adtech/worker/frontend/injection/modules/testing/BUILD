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
    name = "fake_frontend_module",
    srcs = ["FakeFrontendModule.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/injection/modules",
        "//java/com/google/aggregate/adtech/worker/frontend/serialization",
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/converter",
        "//java/com/google/aggregate/adtech/worker/frontend/service/model",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/aws",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/testing",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/validation",
        "//java/com/google/aggregate/adtech/worker/frontend/testing:fake_request_info_validator",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/testing",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:fake_metadata_db",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:job_generator",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules:aws_clients_module",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules/testing:fake_data_module",
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
