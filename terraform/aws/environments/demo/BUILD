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

load("@rules_pkg//:mappings.bzl", "pkg_filegroup", "pkg_files", "pkg_mklink")

package(default_visibility = ["//visibility:public"])

# Files to go both in the "shared" folder and the "demo" folder.
# Not symlinked for simplicity in interacting with various operator
# environments.
SHARED_FILES = [
    # ami_params.auto.tfvars
    "//terraform/aws:ami_params_auto_tfvars",
    "operator_service.tf",
    "operator_service_variables.tf",
]

pkg_files(
    name = "shared_files",
    srcs = SHARED_FILES,
    prefix = "shared",
)

pkg_files(
    name = "demo_files",
    srcs = [
        ".terraform.lock.hcl",
        "README.md",
        "example.auto.tfvars",
        "main.tf",
    ],
    prefix = "demo",
)

# pkg_tar requires links be declared with mklink, otherwise the linked file is
# copied.
pkg_mklink(
    name = "operator_service_link",
    link_name = "demo/operator_service.tf",
    target = "../shared/operator_service.tf",
)

pkg_mklink(
    name = "operator_service_vars_link",
    link_name = "demo/operator_service_variables.tf",
    target = "../shared/operator_service_variables.tf",
)

pkg_mklink(
    name = "ami_params_link",
    link_name = "demo/ami_params.auto.tfvars",
    target = "../shared/ami_params.auto.tfvars",
)

pkg_filegroup(
    name = "demo_environment_pkg",
    srcs = [
        ":ami_params_link",
        ":demo_files",
        ":operator_service_link",
        ":operator_service_vars_link",
        ":shared_files",
    ],
    prefix = "environments",
)
