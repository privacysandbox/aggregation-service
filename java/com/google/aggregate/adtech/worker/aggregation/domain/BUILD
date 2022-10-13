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
    name = "domain",
    srcs = [
        "OutputDomainProcessor.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/perf",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:guava",
        "//java/external:slf4j",
    ],
)

java_library(
    name = "text_domain",
    srcs = ["TextOutputDomainProcessor.java"],
    deps = [
        ":domain",
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/perf",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:guava",
        "//java/external:javax_inject",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/cpio/blobstorageclient",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/cpio/blobstorageclient:model",
    ],
)

java_library(
    name = "avro_domain",
    srcs = ["AvroOutputDomainProcessor.java"],
    deps = [
        ":domain",
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/protocol/avro:avro_output_domain",
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/external:avro",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:guava",
        "//java/external:javax_inject",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/cpio/blobstorageclient",
        "@com_google_adm_cloud_scp//java/com/google/scp/operator/cpio/blobstorageclient:model",
    ],
)
