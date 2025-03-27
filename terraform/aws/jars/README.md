# Operator Jar Output Folder

In the release .tar archive, the jars built for deploying to lambdas are stored in a sibling 'jars'
directory.

This path is recreated locally in order to allow the same relative paths to resolve. Use
build_local_jars.sh to build these jars using bazel and copy them to this directory.

See also: //coordinator/terraform/aws/jars for coordinator jars.
