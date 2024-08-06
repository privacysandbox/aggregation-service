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
    name = "fake_job_result_generator",
    testonly = True,
    srcs = ["FakeJobResultGenerator.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_jobclient_model",
        "//java/external:guava",
        "//java/external:operator_protos",
        "//java/external:scp_shared_proto",
        "//java/external:shared_model",
    ],
)

java_library(
    name = "noop_job_processor",
    testonly = True,
    srcs = ["NoopJobProcessor.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/external:clients_jobclient_model",
    ],
)

java_library(
    name = "fake_record_reader_factory",
    testonly = True,
    srcs = ["FakeRecordReaderFactory.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_jobclient_model",
        "//java/external:guava",
    ],
)

java_library(
    name = "fake_record_decrypter",
    testonly = True,
    srcs = ["FakeRecordDecrypter.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        ":fake_report_generator",
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/external:clients_cryptoclient",
        "//java/external:guava",
    ],
)

java_library(
    name = "fake_report_generator",
    testonly = True,
    srcs = ["FakeReportGenerator.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:guava",
    ],
)

java_library(
    name = "fake_validator",
    testonly = True,
    srcs = [
        "FakeValidator.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:clients_jobclient_model",
        "//java/external:operator_protos",
        "//java/external:shared_model",
    ],
)

java_library(
    name = "fake_decryption_key_service",
    testonly = True,
    srcs = ["FakeDecryptionKeyService.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/selector",
        "//java/external:clients_cryptoclient",
        "//java/external:guava",
        "//java/external:tink",
    ],
)

java_library(
    name = "in_memory_logger",
    srcs = [
        "InMemoryResultLogger.java",
        "InMemoryResultLoggerModule.java",
        "MaterializedAggregationResults.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_jobclient_model",
        "//java/external:guava",
        "//java/external:javax_inject",
    ],
)

java_library(
    name = "avro_results_file_reader",
    srcs = ["AvroResultsFileReader.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/protocol/avro:avro_results_schema_supplier",
        "//java/external:avro",
        "//java/external:guava",
        "//java/external:javax_inject",
    ],
)

java_library(
    name = "local_aggregation_worker_runner",
    srcs = ["LocalAggregationWorkerRunner.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker:worker_runner",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:in_memory_logger",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jcommander",
        "//java/external:tink",
    ],
)
