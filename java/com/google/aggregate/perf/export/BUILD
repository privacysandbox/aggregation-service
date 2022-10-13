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

package(default_visibility = ["//visibility:public"])

java_library(
    name = "no_op_exporter",
    srcs = [
        "NoOpStopwatchExporter.java",
    ],
    deps = [
        "//java/com/google/aggregate/perf",
    ],
)

java_library(
    name = "pain_file_exporter",
    srcs = [
        "PlainFileStopwatchExporter.java",
    ],
    deps = [
        "//java/com/google/aggregate/perf",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)

java_library(
    name = "aws_plain_file_exporter",
    srcs = [
        "AwsStopwatchExporter.java",
    ],
    deps = [
        "//java/com/google/aggregate/perf",
        "//java/external:clients_blobstorageclient_aws",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)
