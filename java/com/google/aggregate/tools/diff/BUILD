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
    name = "result_diff",
    srcs = ["ResultDiffer.java"],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:avro_debug_results_file_reader",
        "//java/com/google/aggregate/adtech/worker/testing:avro_results_file_reader",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jcommander",
    ],
)

java_binary(
    name = "ResultDiffer",
    main_class = "com.google.aggregate.tools.diff.ResultDiffer",
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":result_diff",
    ],
)

java_library(
    name = "diff_runner",
    srcs = [
        "DiffRunner.java",
        "DiffRunnerArgs.java",
        "DiffRunnerResult.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        ":result_diff",
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker:worker_runner",
        "//java/com/google/aggregate/adtech/worker/exceptions",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/testing:avro_results_file_reader",
        "//java/com/google/aggregate/adtech/worker/testing:local_aggregation_worker_runner",
        "//java/com/google/aggregate/adtech/worker/writer",
        "//java/com/google/aggregate/adtech/worker/writer/avro",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:avro",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
        "//java/external:jcommander",
        "//java/external:jimfs",
    ],
)

java_binary(
    name = "DiffRunner",
    main_class = "com.google.aggregate.tools.diff.DiffRunner",
    runtime_deps = [
        ":diff_runner",
        "//java/external:slf4j_simple",
    ],
)
