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

set -eu

ACTION="apply"
AUTO_APPROVE="false"

# Prints usage instructions
print_usage() {
  echo "Usage:  ./operator_deploy.sh --oper_env_path=<environment_path> [--worker_image=<worker_image>] [--action=<action>] [--auto_approve=<true/false>]"
  printf "\n\t--oper_env_path - The operator environment to deploy.\n"
  printf "\t--worker_image  - Artifact registry worker image. Default uses tfvars variable.\n"
  printf "\t--action        - Terraform action to take (apply|destroy). Default is apply.\n"
  printf "\t--auto_approve  - Whether to auto-approve the Terraform action. Default is false.\n"
}

deploy-operator-terraform() {
  WORKER_IMAGE_OPTION=""
  AUTO_APPROVE_OPTION=""

  if [[ -n ${WORKER_IMAGE+x} ]]; then
    WORKER_IMAGE_OPTION=" -var worker_image=$WORKER_IMAGE "
  fi
  if [[ $AUTO_APPROVE == "true" && $ACTION != "plan" ]]; then
    AUTO_APPROVE_OPTION=" -auto-approve "
  fi

  terraform -chdir="$ENV_DIR" init || { echo 'Failed to init operator terraform.' ; exit 1; }
  terraform -- -chdir="$ENV_DIR" "$ACTION" "$AUTO_APPROVE_OPTION" "$WORKER_IMAGE_OPTION" || { echo "Failed to apply $ACTION operator terraform." ; exit 1; }
}

# shellcheck disable=SC2034
FUNCTION=$1
# parse arguments
for ARGUMENT in "$@"
  do
    case $ARGUMENT in
      --worker_image=*)
        WORKER_IMAGE=$(echo "$ARGUMENT" | cut -f2 -d=)
        ;;
      --oper_env_path=*)
        ENV_DIR=$(echo "$ARGUMENT" | cut -f2 -d=)
        ;;
      --action=*)
        input=$(echo "$ARGUMENT" | cut -f2 -d=)
        if [[ $input = "apply" || $input = "destroy" || $input = "plan" ]]
        then
          ACTION=$input
        else
          printf "ERROR: Input action must be one of (apply|destroy|plan).\n"
          exit 1
        fi
        ;;
      --auto_approve=*)
        AUTO_APPROVE=$(echo "$ARGUMENT" | cut -f2 -d=)
        ;;
      help)
        print_usage
        exit 1
        ;;
      *)
        printf "ERROR: invalid argument %s\n" "$ARGUMENT"
        print_usage
        exit 1
        ;;
    esac
done

if [[ -n ${ENV_PATH+x} ]]; then
  printf "ERROR: Operator environment path (OPER_ENV_PATH) must be provided.\n"
  exit 1
fi

deploy-operator-terraform
