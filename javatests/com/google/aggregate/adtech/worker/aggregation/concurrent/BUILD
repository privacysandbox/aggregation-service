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
    name = "ConcurrentAggregationProcessorTest",
    srcs = ["ConcurrentAggregationProcessorTest.java"],
    runtime_deps = ["//telemetry/debug/java/com/google/privacysandbox/otel:otel_debug"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/aggregation/concurrent",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain:avro_domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain:text_domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/engine",
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/model/serdes/cbor",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/testing:fake_decryption_key_service",
        "//java/com/google/aggregate/adtech/worker/testing:fake_record_decrypter",
        "//java/com/google/aggregate/adtech/worker/testing:fake_record_reader_factory",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_generator",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_writer",
        "//java/com/google/aggregate/adtech/worker/testing:fake_validator",
        "//java/com/google/aggregate/adtech/worker/testing:in_memory_logger",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/util:numerical_conversions_utils",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/perf/export:no_op_exporter",
        "//java/com/google/aggregate/privacy/budgeting/bridge:fake_privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/bridge:privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/bridge:unlimited_privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing",
        "//java/com/google/aggregate/protocol/avro:avro_debug_results_schema_supplier",
        "//java/com/google/aggregate/protocol/avro:avro_output_domain",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/com/google/aggregate/protocol/avro:avro_results_schema_supplier",
        "//java/com/google/aggregate/shared/mapper",
        "//java/external:acai",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_cryptoclient",
        "//java/external:clients_distributed_privacy_budgeting",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_databind",
        "//java/external:scp_shared_proto",
        "//java/external:test_parameter_injector",
        "//protos/shared/backend:java_proto",
        "//telemetry/common/java/com/google/privacysandbox/otel:otel_common",
    ],
)

java_test(
    name = "ReportAggregatorTest",
    srcs = ["ReportAggregatorTest.java"],
    runtime_deps = ["//telemetry/debug/java/com/google/privacysandbox/otel:otel_debug"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/aggregation/concurrent",
        "//java/com/google/aggregate/adtech/worker/aggregation/engine",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/model/serdes/cbor",
        "//java/com/google/aggregate/adtech/worker/testing:fake_decryption_key_service",
        "//java/com/google/aggregate/adtech/worker/testing:fake_record_decrypter",
        "//java/com/google/aggregate/adtech/worker/testing:fake_record_reader_factory",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_generator",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_writer",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/perf/export:no_op_exporter",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/external:acai",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_cryptoclient",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
        "//telemetry/common/java/com/google/privacysandbox/otel:otel_common",
    ],
)

java_test(
    name = "ReportReaderTest",
    srcs = ["ReportReaderTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/aggregation/concurrent",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/model/serdes/cbor",
        "//java/com/google/aggregate/adtech/worker/testing:fake_decryption_key_service",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_generator",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_writer",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/external:acai",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_cryptoclient",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
    ],
)
