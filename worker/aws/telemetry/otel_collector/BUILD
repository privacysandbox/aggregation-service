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

load("@com_github_google_rpmpack//:def.bzl", "pkg_tar2rpm")
load("@io_bazel_rules_docker//container:container.bzl", "container_image")
load(
    "@rules_pkg//:mappings.bzl",
    "pkg_attributes",
    "pkg_filegroup",
    "pkg_files",
)
load("@rules_pkg//:pkg.bzl", "pkg_tar")

pkg_files(
    name = "aws_otel_collector_cfg",
    srcs = [
        ":otel_collector_config.yaml",
    ],
    attributes = pkg_attributes(mode = "0444"),
    prefix = "/opt/otel",
)

pkg_files(
    name = "service_cfg",
    srcs = [
        ":otel_collector.service",
    ],
    attributes = pkg_attributes(mode = "0444"),
    prefix = "/etc/systemd/system",
)

pkg_files(
    name = "aws_otel_collector_start_script",
    srcs = [
        ":start_collector.sh",
    ],
    attributes = pkg_attributes(mode = "0555"),
    prefix = "/opt/otel",
)

pkg_files(
    name = "aws_otel_collector_stop_script",
    srcs = [
        ":stop_collector.sh",
    ],
    attributes = pkg_attributes(mode = "0555"),
    prefix = "/opt/otel",
    visibility = ["//worker/aws:__subpackages__"],
)

pkg_filegroup(
    name = "otel_configs_and_scripts",
    srcs = [
        ":aws_otel_collector_cfg",
        ":aws_otel_collector_start_script",
        ":aws_otel_collector_stop_script",
        ":service_cfg",
    ],
    prefix = "/",
)

container_image(
    name = "aws_otel_image",
    base = "@aws_otel_collector//image",
)

pkg_files(
    name = "aws_otel_collector_tar_file",
    srcs = [
        ":aws_otel_image.tar",
    ],
    prefix = "/opt/otel",
    visibility = ["//worker/aws:__subpackages__"],
)

pkg_tar(
    name = "otel_files_tar",
    srcs = [
        ":aws_otel_collector_tar_file",
        ":otel_configs_and_scripts",
    ],
)

pkg_tar2rpm(
    name = "otel_collector_rpm",
    data = ":otel_files_tar",
    pkg_name = "aws_configured_otel_collector",
    postin = """
    sudo systemctl daemon-reload
    sudo systemctl enable otel_collector
    """,
    version = "0.0.1",
    visibility = ["//worker/aws:__subpackages__"],
)
