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

package(default_visibility = ["//visibility:public"])

java_test(
    name = "JobClientImplTest",
    srcs = ["JobClientImplTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/lifecycleclient:local",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/jobqueue/testing",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/common",
        "//java/com/google/aggregate/adtech/worker/shared/dao/metadatadb/testing:fake_metadata_db",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:protobuf_java_util",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/jobqueue:java_proto",
        "//protos/shared/backend/metadatadb:java_proto",
        "@shared_libraries//java/com/google/scp/operator/cpio/metricclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/notificationclient",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:aws",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)

java_test(
    name = "JobValidatorCheckRetryLimitTest",
    srcs = ["JobValidatorCheckRetryLimitTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
    ],
)
