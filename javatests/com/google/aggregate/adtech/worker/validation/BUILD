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
    name = "ReportingOriginIsDomainValidatorTest",
    srcs = ["ReportingOriginIsDomainValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:shared_model",
        "//java/external:test_parameter_injector",
    ],
)

java_test(
    name = "ReportingOriginMatchesRequestValidatorTest",
    srcs = ["ReportingOriginMatchesRequestValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:shared_model",
    ],
)

java_test(
    name = "ReportNotTooOldValidatorTest",
    srcs = ["ReportNotTooOldValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guice",
        "//java/external:shared_model",
    ],
)

java_test(
    name = "ReportForDebugValidatorTest",
    srcs = ["ReportForDebugValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:operator_protos",
        "//java/external:shared_model",
    ],
)

java_test(
    name = "SupportedOperationValidatorTest",
    srcs = ["SupportedOperationValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guice",
    ],
)

java_test(
    name = "SupportedReportApiTypeValidatorTest",
    srcs = ["SupportedReportApiTypeValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker:annotations",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_test(
    name = "ReportPrivacyBudgetKeyFieldsValidatorTest",
    srcs = ["ReportPrivacyBudgetKeyFieldsValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
    ],
)

java_test(
    name = "PrivacyBudgetKeyValidatorFactoryTest",
    srcs = ["PrivacyBudgetKeyValidatorFactoryTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/validation/v01",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
    ],
)

java_test(
    name = "AttributionReportingPrivacyBudgetKeyFieldsValidatorTest",
    srcs = ["v01/AttributionReportingPrivacyBudgetKeyFieldsValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/validation/v01",
        "//java/external:google_truth",
        "//java/external:google_truth8",
    ],
)

java_test(
    name = "AttributionReportingDebugPrivacyBudgetKeyFieldsValidatorTest",
    srcs = ["v01/AttributionReportingDebugPrivacyBudgetKeyFieldsValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/validation/v01",
        "//java/external:google_truth",
        "//java/external:google_truth8",
    ],
)

java_test(
    name = "ProtectedAudiencePrivacyBudgetKeyFieldsValidatorTest",
    srcs = ["v01/ProtectedAudiencePrivacyBudgetKeyFieldsValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/validation/v01",
        "//java/external:google_truth",
        "//java/external:google_truth8",
    ],
)

java_test(
    name = "SharedStoragePrivacyBudgetKeyFieldsValidatorTest",
    srcs = ["v01/SharedStoragePrivacyBudgetKeyFieldsValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/adtech/worker/validation/v01",
        "//java/external:google_truth",
        "//java/external:google_truth8",
    ],
)

java_test(
    name = "SharedInfoReportIdValidatorTest",
    srcs = ["SharedInfoReportIdValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:operator_protos",
        "//java/external:shared_model",
    ],
)

java_test(
    name = "ReportVersionValidatorTest",
    srcs = ["ReportVersionValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:operator_protos",
        "//java/external:shared_model",
    ],
)

java_test(
    name = "JobValidatorTest",
    srcs = ["JobValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:clients_jobclient_aws",
        "//java/external:clients_jobclient_model",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:operator_protos",
        "@shared_libraries//java/com/google/scp/operator/shared/model",
    ],
)
