/**
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

# Example values required by aggregate_service.tf
#
# These values should be modified for each of your environments.

region      = "us-east-1"
environment = "<environment_name>"

instance_type = "m5.2xlarge"

max_job_num_attempts_parameter    = "5"
max_job_processing_time_parameter = "3600"
assume_role_parameter             = "<arn:aws:iam::example:role/example>"

initial_capacity_ec2_instances = 2
min_capacity_ec2_instances     = "1"
max_capacity_ec2_instances     = "20"

alarm_notification_email = "<noreply@example.com>"
