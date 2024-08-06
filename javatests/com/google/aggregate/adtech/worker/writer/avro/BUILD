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

package(default_visibility = ["//visibility:public"])

java_test(
    name = "LocalAvroResultFileWriterTest",
    srcs = ["LocalAvroResultFileWriterTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:avro_results_file_reader",
        "//java/com/google/aggregate/adtech/worker/writer",
        "//java/com/google/aggregate/adtech/worker/writer/avro",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "LocalAvroDebugResultFileWriterTest",
    srcs = ["LocalAvroDebugResultFileWriterTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/writer",
        "//java/com/google/aggregate/adtech/worker/writer/avro",
        "//java/com/google/aggregate/protocol/avro:avro_debug_results",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jimfs",
    ],
)
