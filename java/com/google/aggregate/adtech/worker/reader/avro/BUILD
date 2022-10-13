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
    name = "avro",
    srcs = [
        "LocalNioPathAvroReaderFactory.java",
        "LocalNioPathAvroRecordReader.java",
    ],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:avro",
        "//java/external:aws_s3",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_jobclient_model",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)
