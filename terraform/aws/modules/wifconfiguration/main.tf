/**
 * Copyright 2024 Google LLC
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

locals {
  # The json file template is defined in GCP documentation: go/gcp-wif-config-format
  wif_config_json = jsonencode({
    type                              = "external_account"
    audience                          = "//iam.googleapis.com/projects/${var.gcp_project_number}/locations/global/workloadIdentityPools/${var.workload_identity_pool_id}/providers/${var.workload_identity_pool_provider_id}"
    subject_token_type                = "urn:ietf:params:aws:token-type:aws4_request"
    service_account_impersonation_url = "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/${var.sa_email}:generateAccessToken"
    token_url                         = "https://sts.googleapis.com/v1/token"
    credential_source = {
      environment_id                 = "aws1"
      region_url                     = "http://169.254.169.254/latest/meta-data/placement/availability-zone"
      url                            = "http://169.254.169.254/latest/meta-data/iam/security-credentials"
      regional_cred_verification_url = "https://sts.{region}.amazonaws.com?Action=GetCallerIdentity&Version=2011-06-15"
      imdsv2_session_token_url       = "http://169.254.169.254/latest/api/token"
    }
  })
}

resource "aws_ssm_parameter" "credentials_param" {
  name        = "scp-${var.environment}-${var.coordinator_name}_WIF_CONFIG"
  description = "Google Cloud WIF configuration."
  type        = "String"
  value       = local.wif_config_json
}

resource "aws_ssm_parameter" "service_account_email" {
  name        = "scp-${var.environment}-${var.coordinator_name}_WIF_SA_EMAIL"
  description = "Email address of the service account to be impersonated after successful WIF."
  type        = "String"
  value       = var.sa_email
}
