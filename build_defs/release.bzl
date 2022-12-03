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
load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _s3_jar_release_impl(ctx):
    jar_file = ctx.file.jar_target
    script_template = """#!/bin/bash
set -eux
# parse arguments and set up variables
artifact_base_name="{artifact_base_name}"
version=""
for ARGUMENT in "$@"
do
    case $ARGUMENT in
        --version=*)
        version=$(echo $ARGUMENT | cut -f2 -d=)
        ;;
        *)
        printf "ERROR: invalid argument $ARGUMENT\n"
        exit 1
        ;;
    esac
done
## check variables and arguments
if [[ "$version" == "" ]]; then
    printf "ERROR: --version argument is not provided\n"
    exit 1
fi
if [[ "$artifact_base_name" != *"{{VERSION}}"* ]]; then
    printf "ERROR: artifact_base_name must include {{VERSION}} substring\n"
    exit 1
fi
artifact_name=$(echo $artifact_base_name | sed -e "s/{{VERSION}}/$version/g")
# upload artifact to s3
echo "Preparing release jar to s3"
aws --version
echo "bazel working directory:"
pwd
echo "-------------------------"
outputfile=$(mktemp)
aws s3 cp {jar_file} s3://{release_bucket}/{release_key}/$version/$artifact_name 2>&1 | tee $outputfile
if grep -q "Skipping" "$outputfile"; then
  echo "ERROR: Artifact already exists"
  exit 1
fi
"""
    script = ctx.actions.declare_file("%s_script.sh" % ctx.label.name)
    script_content = script_template.format(
        jar_file = jar_file.short_path,
        release_bucket = ctx.attr.release_bucket[BuildSettingInfo].value,
        release_key = ctx.attr.release_key[BuildSettingInfo].value,
        artifact_base_name = ctx.attr.artifact_base_name,
    )
    ctx.actions.write(script, script_content, is_executable = True)
    runfiles = ctx.runfiles(files = [
        jar_file,
    ])
    return [DefaultInfo(
        executable = script,
        files = depset([script, jar_file]),
        runfiles = runfiles,
    )]

s3_jar_release_rule = rule(
    implementation = _s3_jar_release_impl,
    attrs = {
        "artifact_base_name": attr.string(
            mandatory = True,
        ),
        "jar_target": attr.label(
            allow_single_file = True,
            mandatory = True,
            providers = [BuildSettingInfo],
        ),
        "release_bucket": attr.label(
            mandatory = True,
            providers = [BuildSettingInfo],
        ),
        "release_key": attr.label(
            mandatory = True,
            providers = [BuildSettingInfo],
        ),
    },
    executable = True,
)

def s3_jar_release(
        *,
        name,
        jar_target = ":jar_target_flag",
        release_bucket = ":bucket_flag",
        release_key = ":bucket_path_flag",
        artifact_base_name):
    """
    Creates targets for releasing aggregation service artifact to s3.
    This rule only accepts releasing one artifact (e.g. a jar file). The path in which the artifact is stored
    is specified by the s3 bucket name, key, artifact base name, and version. The naming convention is as below:
    `s3://bucket/key/VERSION/name_of_artifact_VERSION.tgz`
    targets:
      1. '%s_script.sh': script for running aws cli to copy code package to s3.
      2. artifact file: the package file being uploaded to s3. This is built from the
        specified build target (e.g. a `pkg_tar` target).
    Args:
        name: The target name used to generate the targets described above.
        jar_target: Path to the jar build target.
        release_bucket: s3 bucket to which the release the artifact.
        release_key: s3 key for the release artifact.
        artifact_base_name: base name of the artifact. The base name should include a "{VERSION}" substring which will be
            replaced with the version argument. e.g. "AwsChangeHandlerLambda-{VERSION}.tgz".
        gcloud_sdk: Path to google cloud sdk for linux.
    """
    s3_jar_release_rule(
        name = name,
        jar_target = jar_target,
        release_bucket = release_bucket,
        release_key = release_key,
        artifact_base_name = artifact_base_name,
    )
