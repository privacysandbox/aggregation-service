# Copyright 2022 Google LLC
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
    name = "NumericConversionsTest",
    srcs = ["NumericConversionsTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util:numerical_conversions_utils",
        "//java/external:google_truth",
        "//java/external:guava",
    ],
)

java_test(
    name = "DebugSupportHelperTest",
    srcs = ["DebugSupportHelperTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_cryptoclient",
        "//java/external:google_truth",
        "//java/external:guava",
        "//protos/shared/backend:java_proto",
    ],
)

java_test(
    name = "JobResultHelperTest",
    srcs = ["JobResultHelperTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker:annotations",
        "//java/com/google/aggregate/adtech/worker:return_code",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:scp_shared_proto",
        "//protos/shared/backend:java_proto",
        "@com_google_protobuf//:protobuf_java",
    ],
)

java_test(
    name = "OutputShardFileHelperTest",
    srcs = ["OutputShardFileHelperTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:google_truth",
    ],
)

java_test(
    name = "ReportingOriginUtilsTest",
    srcs = ["ReportingOriginUtilsTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:google_truth",
        "//java/external:guava",
    ],
)
