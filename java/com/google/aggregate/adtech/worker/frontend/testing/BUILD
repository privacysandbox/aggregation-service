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
    name = "fake_request_info_validator",
    srcs = ["FakeRequestInfoValidator.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/tasks/validation",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//protos/shared/backend:java_proto",
    ],
)

java_library(
    name = "fake_jobmetadata_change_handler",
    srcs = [
        "FakeJobMetadataChangeHandler.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws/changehandler",
        "//java/external:guava",
        "//protos/shared/backend/metadatadb:java_proto",
    ],
)

java_library(
    name = "testing",
    srcs = [
        "ResponseEventAssertions.java",
        "TestCreateRequest.java",
        "TestCreateResponse.java",
        "TestSerializedObject.java",
        "UnknownErrorResponse.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws",
        "//java/com/google/aggregate/adtech/worker/frontend/tasks",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:aws_lambda_java_core",
        "//java/external:aws_lambda_java_events",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:jackson_annotations",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:protobuf_java",
        "//java/external:protobuf_java_util",
        "@shared_libraries//java/com/google/scp/shared/api/model",
        "@shared_libraries//java/com/google/scp/shared/mapper",
        "@shared_libraries//shared/protos/api/v1:java_proto",
    ],
)

java_library(
    name = "aws_failed_job_queue_write_cleanup_test_module",
    testonly = True,
    srcs = ["AwsFailedJobQueueWriteCleanupTestModule.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/frontend/injection/modules",
        "//java/com/google/aggregate/adtech/worker/frontend/service/aws/changehandler",
        "//java/external:autoservice",
        "//java/external:autoservice_annotations",
        "//java/external:aws_apache_http",
        "//java/external:aws_dynamodb",
        "//java/external:aws_dynamodb_enhanced",
        "//java/external:guava",
        "//java/external:guice",
    ],
)
