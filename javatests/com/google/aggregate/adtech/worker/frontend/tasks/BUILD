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

load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "GetJobTaskTest",
    srcs = ["GetJobTaskTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/injection/modules/testing:fake_frontend_module",
        "//java/com/google/aggregate/adtech/worker/frontend/service",
        "//java/com/google/aggregate/adtech/worker/frontend/service/converter",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/validation",
        "//java/com/google/aggregate/adtech/worker/frontend/testing:fake_request_info_validator",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:fake_metadata_db",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:job_generator",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules:aws_clients_module",
        "//java/com/google/aggregate/adtech/worker/shared/injection/modules/testing:fake_data_module",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/shared/testing",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//java/com/google/scp/shared/api/exception",
        "@shared_libraries//java/com/google/scp/shared/api/exception/testing",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)
