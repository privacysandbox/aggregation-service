load("@bazel_skylib//rules:common_settings.bzl", "BuildSettingInfo")

def _ami_params_tfvar_file_impl(ctx):
    tfvar_file = ctx.actions.declare_file(ctx.attr.file_name)
    file_contents = "\n".join([
        "/**",
        " * Copyright 2022 Google LLC",
        " *",
        " * Licensed under the Apache License, Version 2.0 (the \"License\");",
        " * you may not use this file except in compliance with the License.",
        " * You may obtain a copy of the License at",
        " *",
        " *      http://www.apache.org/licenses/LICENSE-2.0",
        " *",
        " * Unless required by applicable law or agreed to in writing, software",
        " * distributed under the License is distributed on an \"AS IS\" BASIS,",
        " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
        " * See the License for the specific language governing permissions and",
        " * limitations under the License.",
        " */",
        "# AMI tested to work with this release, not recommended to change.",
        "ami_name = \"%s\"" % ctx.attr.ami_name_flag[BuildSettingInfo].value,
        # Note: ami_owner is an array so don't quote the value.
        "ami_owners = %s" % ctx.attr.ami_owners_flag[BuildSettingInfo].value,
    ])
    ctx.actions.write(tfvar_file, file_contents)

    return DefaultInfo(
        files = depset([tfvar_file]),
    )

ami_params_tfvar_file = rule(
    doc = """Generates an ami_params.auto.tfvars file using build flag values.

    Creates a simple .tfvars file which is meant to be included in an exported
    tar file release which incldues the necessary params for a given release
    that might not be known by this repo.

    Example:
      ami_params_tfvar(name = "foo", file_name = "ami_params.auto.tfvars")
      # bazel build :foo --//terraform/aws/applications/operator-service:ami_name_flag="my-ami-name"

    Attrs:
      file_name: name of the generated tfvars file. Recommended to use
        "ami_params.auto.tfvars"
    """,
    implementation = _ami_params_tfvar_file_impl,
    attrs = {
        "ami_name_flag": attr.label(
            providers = [BuildSettingInfo],
            doc = "Bazel string_flag which provides the value for the ami name.",
            default = "//terraform/aws/applications/operator-service:ami_name_flag",
        ),
        "ami_owners_flag": attr.label(
            providers = [BuildSettingInfo],
            doc = "Bazel string_flag which provides the value for the ami owner.",
            default = "//terraform/aws/applications/operator-service:ami_owners_flag",
        ),
        "file_name": attr.string(
            mandatory = True,
            default = "ami_params.auto.tfvars",
        ),
    },
)
