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
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:google_truth",
        "//java/external:guava",
        "//java/external:test_parameter_injector",
    ],
)

java_test(
    name = "ReportingOriginMatchesRequestValidatorTest",
    srcs = ["ReportingOriginMatchesRequestValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
    ],
)

java_test(
    name = "ReportNotTooOldValidatorTest",
    srcs = ["ReportNotTooOldValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guice",
    ],
)

java_test(
    name = "ReportForDebugValidatorTest",
    srcs = ["ReportForDebugValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
        "//protos/shared/backend:java_proto",
    ],
)

java_test(
    name = "SupportedOperationValidatorTest",
    srcs = ["SupportedOperationValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
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
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:acai",
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
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/util",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/com/google/aggregate/privacy/budgeting/budgetkeygenerator:privacy_budget_key_generator",
        "//java/external:acai",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//java/external:guava",
        "//java/external:guice",
    ],
)

java_test(
    name = "SharedInfoReportIdValidatorTest",
    srcs = ["SharedInfoReportIdValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//protos/shared/backend:java_proto",
    ],
)

java_test(
    name = "ReportVersionValidatorTest",
    srcs = ["ReportVersionValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:google_truth",
        "//java/external:google_truth8",
        "//protos/shared/backend:java_proto",
    ],
)

java_test(
    name = "JobValidatorTest",
    srcs = ["JobValidatorTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/jobclient:aws",
        "//java/com/google/aggregate/adtech/worker/jobclient:model",
        "//java/com/google/aggregate/adtech/worker/shared/model",
        "//java/com/google/aggregate/adtech/worker/validation",
        "//java/external:google_truth",
        "//java/external:guava",
        "//protos/shared/backend:java_proto",
    ],
)
