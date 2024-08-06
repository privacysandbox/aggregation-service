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
    name = "cloud",
    srcs = [
        "CloudEncryptionKeyModule.java",
        "CloudEncryptionKeyService.java",
    ],
    javacopts = ["-Xep:Var"],
    deps = [
        "//java/com/google/aggregate/adtech/worker/encryption/hybrid/key",
        "//java/com/google/aggregate/shared/mapper",
        "//java/external:apache_httpclient",
        "//java/external:apache_httpcore",
        "//java/external:api_shared_util",
        "//java/external:guava",
        "//java/external:guice",
        "//java/external:jackson_core",
        "//java/external:jackson_databind",
        "//java/external:javax_inject",
        "//java/external:keymanagement_keyhosting_java_proto",
        "//java/external:keymanagement_shared_util",
        "//java/external:protobuf_java_util",
        "//java/external:tink",
        "//protocol/proto:encryption_key_config_java_proto",
    ],
)
