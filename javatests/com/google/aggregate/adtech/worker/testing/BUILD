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

java_test(
    name = "FakeJobResultGeneratorTest",
    srcs = ["FakeJobResultGeneratorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:fake_job_result_generator",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:operator_protos",
        "//java/external:scp_shared_proto",
        "//java/external:shared_model",
    ],
)

java_test(
    name = "NoopJobProcessorTest",
    srcs = ["NoopJobProcessorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:fake_job_result_generator",
        "//java/com/google/aggregate/adtech/worker/testing:noop_job_processor",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
    ],
)

java_test(
    name = "FakeRecordReaderFactoryTest",
    srcs = ["FakeRecordReaderFactoryTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:fake_record_reader_factory",
        "//java/external:clients_blobstorageclient_aws",
        "//java/external:clients_blobstorageclient_model",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
    ],
)

java_test(
    name = "FakeRecordDecrypterTest",
    srcs = ["FakeRecordDecrypterTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:fake_record_decrypter",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_generator",
        "//java/external:clients_cryptoclient",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
    ],
)

java_test(
    name = "FakeReportGeneratorTest",
    srcs = ["FakeReportGeneratorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_generator",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/external:google_truth",
        "//java/external:guava",
    ],
)

java_test(
    name = "FakeValidatorTest",
    srcs = ["FakeValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:fake_report_generator",
        "//java/com/google/aggregate/adtech/worker/testing:fake_validator",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
    ],
)

java_test(
    name = "FakeDecryptionKeyServiceTest",
    srcs = ["FakeDecryptionKeyServiceTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/testing:fake_decryption_key_service",
        "//java/external:clients_cryptoclient",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:tink",
    ],
)

java_test(
    name = "InMemoryResultLoggerTest",
    srcs = ["InMemoryResultLoggerTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:in_memory_logger",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:guava",
    ],
)

java_test(
    name = "AvroResultsFileReaderTest",
    srcs = ["AvroResultsFileReaderTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:avro_results_file_reader",
        "//java/com/google/aggregate/adtech/worker/writer/avro",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jimfs",
    ],
)
