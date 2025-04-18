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

load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "domain",
    srcs = [
        "OutputDomainProcessor.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/aggregation/engine",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise:params",
        "//java/com/google/aggregate/privacy/noise/model",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:guava",
        "//java/external:rxjava",
        "//java/external:slf4j",
    ],
)

java_library(
    name = "text_domain",
    srcs = ["TextOutputDomainProcessor.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        ":domain",
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/util:numerical_conversions_utils",
        "//java/com/google/aggregate/perf",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:guava",
        "//java/external:javax_inject",
        "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient:model",
    ],
)

java_library(
    name = "avro_domain",
    srcs = ["AvroOutputDomainProcessor.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        ":domain",
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/protocol/avro:avro_output_domain",
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/external:avro",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:guava",
        "//java/external:javax_inject",
        "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient",
        "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient:model",
    ],
)
