# `frontend`

This module contains resources for the frontend service lambdas and their supporting resources.

## Lambda jars

This expects that paths to local jars are passed as arguments. Since these will be built by bazel it
is recommended that any environments that import this module have their own `bazel_bin_path`
variable that is set to the value of `bazel info bazel-bin`. This variable can then be used to build
paths to the local jar from the bazel bin root, as paths below that root are predictable.

To run terraform commands with this variable set, add `-var=bazel_bin_path=$(bazel info bazel-bin)`
to the terraform command. Example below

```bash
terraform plan -var=bazel_bin_path=$(bazel info bazel-bin)
terraform apply -var=bazel_bin_path=$(bazel info bazel-bin)
```
