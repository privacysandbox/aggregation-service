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
    name = "concurrent",
    srcs = [
        "ConcurrentAggregationProcessor.java",
        "ReportAggregator.java",
        "ReportReader.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/engine",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model:privacy_budget_debugging_model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/util:numerical_conversions_utils",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/privacy/budgeting/bridge:privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise:params",
        "//java/com/google/aggregate/privacy/noise/model",
        "//java/com/google/aggregate/protocol/avro:avro_record_reader",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/external:avro",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_distributed_privacy_budgeting",
        "//java/external:clients_jobclient_model",
        "//java/external:error_prone_anntotations",
        "//java/external:guava",
        "//java/external:javax_inject",
        "//java/external:operator_protos",
        "//java/external:rxjava",
        "//java/external:scp_shared_proto",
        "//java/external:shared_model",
        "//java/external:slf4j",
        "//telemetry/common/java/com/google/privacysandbox/otel:otel_common",
    ],
)
