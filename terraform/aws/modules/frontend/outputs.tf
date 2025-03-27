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

output "frontend_lambda_role_arn" {
  value = aws_iam_role.frontend_lambda_role.arn
}

output "frontend_lambda_log_attachment" {
  value = aws_iam_role_policy_attachment.frontend_lambda_log_policy_attachment
}

output "frontend_lambda_role_id" {
  value = aws_iam_role.frontend_lambda_role.id
}

output "frontend_api_id" {
  value = aws_apigatewayv2_api.frontend_api_gateway.id
}

output "create_job_endpoint" {
  value = replace(aws_apigatewayv2_route.create_job_api_gateway_route.route_key, "/${var.api_version}", "${aws_apigatewayv2_api.frontend_api_gateway.api_endpoint}/${var.api_env_stage_name}/${var.api_version}")
}

output "get_job_endpoint" {
  value = replace(aws_apigatewayv2_route.get_job_api_gateway_route.route_key, "/${var.api_version}", "${aws_apigatewayv2_api.frontend_api_gateway.api_endpoint}/${var.api_env_stage_name}/${var.api_version}")
}
