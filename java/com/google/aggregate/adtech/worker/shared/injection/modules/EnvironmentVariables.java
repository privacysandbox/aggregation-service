/*
 * Copyright 2025 Google LLC
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

package com.google.aggregate.adtech.worker.shared.injection.modules;

/** Contains environment variable keys used by the {@code DataModule} class. */
public final class EnvironmentVariables {
  /** The AWS region used by the deployment. */
  public static String AWS_REGION_ENV_VAR = "AWS_REGION";

  /** The name of the DynamoDB job metadata table. */
  public static String JOB_METADATA_TABLE_ENV_VAR = "JOB_METADATA_TABLE";

  /** The name of the TTL variable for the job metadata table. */
  public static String JOB_METADATA_TTL_ENV_VAR = "JOB_METADATA_TTL";

  /**
   * The endpoint URL environment variable supplied by LocalStack, used to connect to the AWS SDK.
   */
  public static String AWS_ENDPOINT_URL_ENV_VAR = "AWS_ENDPOINT_URL";
}
