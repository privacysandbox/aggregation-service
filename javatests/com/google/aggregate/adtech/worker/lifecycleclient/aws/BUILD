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
    name = "AwsLifecycleClientTest",
    srcs = ["AwsLifecycleClientTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/lifecycleclient:aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/testing",
        "//java/external:aws_auto_scaling",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:guice-testlib",
        "//java/external:mockito",
        "//protos/shared/backend/asginstance:java_proto",
        "@shared_libraries//java/com/google/scp/operator/cpio/configclient:aws",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:aws",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)

java_test(
    name = "AwsLifecycleActionExtenderServiceTest",
    srcs = ["AwsLifecycleActionExtenderServiceTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/lifecycleclient:aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/aws",
        "//java/com/google/aggregate/adtech/worker/shared/dao/asginstancesdb/common",
        "//java/com/google/aggregate/adtech/worker/shared/testing",
        "//java/external:aws_auto_scaling",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:guice-testlib",
        "//java/external:mockito",
        "//java/external:protobuf_java",
        "//protos/shared/backend:java_proto",
        "//protos/shared/backend/asginstance:java_proto",
        "@shared_libraries//java/com/google/scp/operator/cpio/configclient:aws",
        "@shared_libraries//java/com/google/scp/shared/clients/configclient:aws",
        "@shared_libraries//java/com/google/scp/shared/proto",
    ],
)
