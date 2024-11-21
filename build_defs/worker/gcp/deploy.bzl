# Copyright 2023 Google LLC
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

load("@io_bazel_rules_docker//container:container.bzl", "container_image", "container_push")

def worker_gcp_deployment(
        *,
        name,
        cmd = [],
        files = [],
        entrypoint = [],
        labels = {}):
    docker_target_name = "%s_container" % name

    container_image(
        name = docker_target_name,
        base = "@java_base//image",
        cmd = cmd,
        entrypoint = entrypoint,
        files = files,
        labels = labels,
    )

    # registry,repository,tag will be assigned by "-dst" argument.
    container_push(
        name = name,
        format = "Docker",
        image = docker_target_name,
        registry = "",
        repository = "",
        tag = "",
    )
