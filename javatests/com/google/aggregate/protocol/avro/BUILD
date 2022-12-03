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
    name = "AvroOutputDomainSchemaSupplierTest",
    srcs = ["AvroOutputDomainSchemaSupplierTest.java"],
    deps = [
        "//java/com/google/aggregate/protocol/avro:avro_output_domain_schema_supplier",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:guava",
    ],
)

java_test(
    name = "AvroReportsSchemaSupplierTest",
    srcs = ["AvroReportsSchemaSupplierTest.java"],
    deps = [
        "//java/com/google/aggregate/protocol/avro:avro_reports_schema_supplier",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:guava",
    ],
)

java_test(
    name = "AvroOutputDomainReaderTest",
    srcs = ["AvroOutputDomainReaderTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/protocol/avro:avro_output_domain",
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/com/google/aggregate/protocol/avro:avro_record_writer",
        "//java/external:acai",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "AvroReportsReaderTest",
    srcs = ["AvroReportsReaderTest.java"],
    deps = [
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/com/google/aggregate/protocol/avro:avro_record_writer",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/external:acai",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "AvroDebugResultsReaderTest",
    srcs = ["AvroDebugResultsReaderTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/protocol/avro:avro_debug_results",
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/com/google/aggregate/protocol/avro:avro_record_writer",
        "//java/external:acai",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "AvroOutputDomainWriterTest",
    srcs = ["AvroOutputDomainWriterTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/protocol/avro:avro_output_domain",
        "//java/com/google/aggregate/protocol/avro:avro_record_writer",
        "//java/external:acai",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "AvroReportWriterTest",
    srcs = ["AvroReportWriterTest.java"],
    deps = [
        "//java/com/google/aggregate/protocol/avro:avro_record_writer",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/external:acai",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "AvroDebugResultsWriterTest",
    srcs = ["AvroDebugResultsWriterTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/protocol/avro:avro_debug_results",
        "//java/com/google/aggregate/protocol/avro:avro_record_writer",
        "//java/external:acai",
        "//java/external:avro",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jimfs",
    ],
)

java_test(
    name = "AvroResultsSchemaSupplierTest",
    srcs = ["AvroResultsSchemaSupplierTest.java"],
    deps = [
        "//java/com/google/aggregate/protocol/avro:avro_results_schema_supplier",
        "//java/external:avro",
        "//java/external:google_truth",
    ],
)

java_test(
    name = "AvroDebugResultsSchemaSupplierTest",
    srcs = ["AvroDebugResultsSchemaSupplierTest.java"],
    deps = [
        "//java/com/google/aggregate/protocol/avro:avro_debug_results_schema_supplier",
        "//java/external:avro",
        "//java/external:google_truth",
    ],
)
