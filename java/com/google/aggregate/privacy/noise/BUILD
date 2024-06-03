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
    name = "noise",
    srcs = [
        "Annotations.java",
        "DiscreteNoise.java",
        "DpNoiseApplier.java",
        "DpNoiseParamsFactory.java",
        "DpNoiseParamsModule.java",
        "DpNoisedAggregationModule.java",
        "LaplaceDiscreteNoise.java",
        "NoiseApplier.java",
        "NoisedAggregationRunner.java",
        "NoisedAggregationRunnerImpl.java",
        "ThresholdSupplier.java",
        "ThresholdSupplierModule.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker:annotations",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/privacy/noise/model",
        "//java/com/google/aggregate/privacy/noise/proto:privacy_parameters_java_proto",
        "//java/external:autovalue",
        "//java/external:autovalue_annotations",
        "//java/external:differential_privacy",
        "//java/external:error_prone_anntotations",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:javax_inject",
    ],
)
