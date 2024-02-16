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
    name = "RecordEncrypterImplTest",
    srcs = ["RecordEncrypterImplTest.java"],
    deps = [
        "//java/com/google/aggregate/adtech/worker",
        "//java/com/google/aggregate/adtech/worker/decryption",
        "//java/com/google/aggregate/adtech/worker/decryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/encryption",
        "//java/com/google/aggregate/adtech/worker/encryption:encrypter",
        "//java/com/google/aggregate/adtech/worker/encryption/hybrid",
        "//java/com/google/aggregate/adtech/worker/encryption/hybrid/key",
        "//java/com/google/aggregate/adtech/worker/encryption/hybrid/key/cloud",
        "//java/com/google/aggregate/adtech/worker/encryption/hybrid/key/testing",
        "//java/com/google/aggregate/adtech/worker/model",
        "//java/com/google/aggregate/adtech/worker/model/serdes",
        "//java/com/google/aggregate/adtech/worker/model/serdes/cbor",
        "//java/external:acai",
        "//java/external:api_shared_util",
        "//java/external:google_truth",
        "//java/external:google_truth_proto",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:shared_testing",
        "//java/external:tink",
    ],
)
