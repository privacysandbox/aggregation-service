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
    name = "GcpCreateJobTaskTest",
    srcs = ["GcpCreateJobTaskTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/tasks",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/gcp",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/validation",
        "//java/com/google/aggregate/adtech/worker/frontend/testing:fake_request_info_validator",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/testing",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:fake_metadata_db",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:job_generator",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/shared/testing",
        "//java/external:acai",
        "//java/external:google_truth",
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
