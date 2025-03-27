#!/usr/bin/env bash
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

#
# Builds the *operator* jars using bazel and copies them to the current working
# directory.
#
# This allows Terraform applications to refer to jars via relative path in a way
# consistent with our deployment tar file.

set -eux

# Output tar file from the jar_tar build rule.
TAR_PATH="terraform/aws/jars/jar_tar.tar"
TAR_TARGET="//terraform/aws/jars:jar_tar"

bazel build "${TAR_TARGET}"

rm *.jar || echo "no jars to clean up"
rm -r jars/ || echo "no jar directory to clean up"

tar xf $(bazel info bazel-bin)/$TAR_PATH

mv jars/* .
rmdir jars

set +x
printf "\n[Warning]: These Jar files are built from HEAD and not from a specific release!\n\n"
