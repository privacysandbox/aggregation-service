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

load("@pip_deps//:requirements.bzl", "requirement")

py_library(
    name = "aggregation_service_load_tests",
    srcs = glob(
        ["*.py"],
        allow_empty = False,
        exclude = ["*_test.py"],
    ),
    deps = [requirement("urllib3")],
)

[
    py_test(
        name = test.replace(".py", ""),
        srcs = [test],
        python_version = "PY3",
        tags = ["manual"],
        deps = [
            ":aggregation_service_load_tests",
        ],
    )
    for test in glob(["*_test.py"])
]

test_suite(
    name = "unit_tests",
    tags = ["manual"],
    tests = [test.replace(".py", "") for test in glob(["*_test.py"])],
)
