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

load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "worker_util",
    srcs = [
        "AggregationWorker.java",
        "AggregationWorkerReturnCode.java",
        "Annotations.java",
        "DecryptionModuleSelector.java",
        "DomainFormatSelector.java",
        "JobProcessor.java",
        "LocalFileToCloudStorageLogger.java",
        "LocalFileToCloudStorageLoggerModule.java",
        "NoisingSelector.java",
        "PrivacyBudgetingSelector.java",
        "RecordReader.java",
        "RecordReaderFactory.java",
        "ResultLogger.java",
        "ResultLoggerModule.java",
        "ResultLoggerModuleSelector.java",
        "StopwatchExportSelector.java",
        "WorkerModule.java",
        "WorkerPullWorkService.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/aggregation/domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain:avro_domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain:text_domain",
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/decryption/noop",
        "//java/com/google/aggregate/adtech/worker/encryption",
        "//java/com/google/aggregate/adtech/worker/encryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:in_memory_logger",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/writer",
        "//java/com/google/aggregate/adtech/worker/writer/avro",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/perf/export:aws_plain_file_exporter",
        "//java/com/google/aggregate/perf/export:no_op_exporter",
        "//java/com/google/aggregate/perf/export:pain_file_exporter",
        "//java/com/google/aggregate/privacy/budgeting/bridge:http_privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/bridge:privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/bridge:unlimited_privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/external:clients_blobstorageclient",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_jobclient",
        "//java/external:clients_jobclient_model",
        "//java/external:clients_metricclient",
        "//java/external:clients_metricclient_model",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:operator_protos",
        "//java/external:scp_shared_proto",
        "//java/external:shared_model",
        "//java/external:slf4j",
        "//telemetry/common/java/com/google/privacysandbox/otel:otel_common",
    ],
)

java_library(
    name = "worker",
    srcs = [
        "AggregationWorker.java",
        "AggregationWorkerReturnCode.java",
        "Annotations.java",
        "ErrorSummaryAggregator.java",
        "JobProcessor.java",
        "LocalFileToCloudStorageLogger.java",
        "LocalFileToCloudStorageLoggerModule.java",
        "RecordReader.java",
        "RecordReaderFactory.java",
        "ReportDecrypterAndValidator.java",
        "ResultLogger.java",
        "ResultLoggerModule.java",
        "WorkerModule.java",
        "WorkerPullWorkService.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/aggregation/engine",
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/encryption",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/writer",
        "//java/com/google/aggregate/adtech/worker/writer/avro",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/privacy/budgeting/bridge:privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/protocol/avro:avro_report",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:aws_regions",
        "//java/external:clients_blobstorageclient_aws",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_cryptoclient",
        "//java/external:clients_jobclient",
        "//java/external:clients_jobclient_model",
        "//java/external:clients_lifecycleclient_aws",
        "//java/external:clients_metricclient_aws",
        "//java/external:clients_metricclient_model",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:operator_protos",
        "//java/external:scp_shared_proto",
        "//java/external:shared_model",
        "//java/external:slf4j",
        "//telemetry/common/java/com/google/privacysandbox/otel:otel_common",
    ],
)

java_library(
    name = "worker_runner",
    srcs = [
        "AggregationWorkerArgs.java",
        "AggregationWorkerModule.java",
        "AggregationWorkerRunner.java",
        "DecryptionModuleSelector.java",
        "DomainFormatSelector.java",
        "LibraryAnnotations.java",
        "LocalAvroResultLoggerModule.java",
        "LocalJsonResultLoggerModule.java",
        "LocalResultLogger.java",
        "LocalRunner.java",
        "LocalWorkerArgs.java",
        "LocalWorkerModule.java",
        "NoisingSelector.java",
        "PrivacyBudgetingSelector.java",
        "RecordReaderSelector.java",
        "ResultLoggerModuleSelector.java",
        "StopwatchExportSelector.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":worker",
        "//java/com/google/aggregate/adtech/worker/aggregation/concurrent",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain:avro_domain",
        "//java/com/google/aggregate/adtech/worker/aggregation/domain:text_domain",
        "//java/com/google/aggregate/adtech/worker/configs",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/decryption/noop",
        "//java/com/google/aggregate/adtech/worker/encryption",
        "//java/com/google/aggregate/adtech/worker/encryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/local:localblob_client",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/model/serdes/cbor",
        "//java/com/google/aggregate/adtech/worker/reader/avro",
        "//java/com/google/aggregate/adtech/worker/selector",
        "//java/com/google/aggregate/adtech/worker/testing:in_memory_logger",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/writer",
        "//java/com/google/aggregate/adtech/worker/writer/avro",
        "//java/com/google/aggregate/adtech/worker/writer/json",
        "//java/com/google/aggregate/perf",
        "//java/com/google/aggregate/perf/export:aws_plain_file_exporter",
        "//java/com/google/aggregate/perf/export:no_op_exporter",
        "//java/com/google/aggregate/perf/export:pain_file_exporter",
        "//java/com/google/aggregate/privacy/budgeting/bridge:http_privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/bridge:privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/bridge:unlimited_privacy_budgeting_service_bridge",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/com/google/aggregate/privacy/noise",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/com/google/aggregate/privacy/noise/testing",
        "//java/com/google/aggregate/shared",
        "//java/com/google/aggregate/shared/mapper",
        "//java/external:aws_apache_http",
        "//java/external:aws_http_client_spi",
        "//java/external:aws_regions",
        "//java/external:aws_s3",
        "//java/external:clients_blobstorageclient_aws",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_configclient",
        "//java/external:clients_configclient_aws",
        "//java/external:clients_configclient_aws_operator",
        "//java/external:clients_configclient_model",
        "//java/external:clients_configclient_operator",
        "//java/external:clients_cryptoclient",
        "//java/external:clients_cryptoclient_aws",
        "//java/external:clients_distributed_privacy_budgeting",
        "//java/external:clients_distributed_privacy_budgeting_aws",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:clients_lifecycleclient_aws",
        "//java/external:clients_metricclient_aws",
        "//java/external:dao_jobqueue_aws",
        "//java/external:dao_metadatadb_aws",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_databind",
        "//java/external:javax_inject",
        "//java/external:jcommander",
        "//java/external:slf4j",
        "//telemetry/common/java/com/google/privacysandbox/otel:otel_common",
    ],
)

java_library(
    name = "return_code",
    srcs = [
        "AggregationWorkerReturnCode.java",
    ],
    javacopts = ["-Xep:Var"],
)

java_library(
    name = "annotations",
    srcs = [
        "Annotations.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/external:guice",
    ],
)

# The uber-JAR for the aggregation library.
java_binary(
    name = "LocalTestingTool",
    classpath_resources = ["//java/com/google/aggregate:slf4jconfig_prod"],
    main_class = "com.google.aggregate.adtech.worker.LocalRunner",
    resources = [
        "//licenses",
    ],
    runtime_deps = [
        # //telemetry needs to be before worker
        "//telemetry/debug/java/com/google/privacysandbox/otel:otel_debug",
        ":worker_runner",
        "//java/external:commons_logging",
        "//java/external:slf4j_simple",
    ],
)

java_binary(
    name = "WorkerRunner_debug",
    classpath_resources = ["//java/com/google/aggregate:slf4jconfig_prod"],
    main_class = "com.google.aggregate.adtech.worker.AggregationWorkerRunner",
    runtime_deps = [
        # //telemetry library should be before :worker_runner
        "//telemetry/debug/java/com/google/privacysandbox/otel:otel_debug",
        ":worker_runner",
        "//java/external:commons_logging",
        "//java/external:slf4j_simple",
    ],
)

java_binary(
    name = "WorkerRunner_prod",
    classpath_resources = ["//java/com/google/aggregate:slf4jconfig_prod"],
    main_class = "com.google.aggregate.adtech.worker.AggregationWorkerRunner",
    runtime_deps = [
        # //telemetry library should be before :worker_runner
        "//telemetry/prod/java/com/google/privacysandbox/otel:otel_prod",
        ":worker_runner",
        "//java/external:commons_logging",
        "//java/external:slf4j_simple",
    ],
)

# The uber-JAR for the LocalTestingTool.
alias(
    name = "LocalTestingToolDeploy",
    actual = ":LocalTestingTool_deploy.jar",
)

# The uber-JAR for the aggregation worker.
alias(
    name = "WorkerRunnerDeploy",
    actual = ":WorkerRunner_prod_deploy.jar",
)

# The uber-JAR for the aggregation worker.
alias(
    name = "WorkerRunnerDeployDebug",
    actual = ":WorkerRunner_debug_deploy.jar",
)
