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

# Define API Gateway properties
resource "aws_apigatewayv2_api" "frontend_api_gateway" {
  name          = var.frontend_api_name
  protocol_type = "HTTP"
  description   = var.api_description
}

# Define routes
resource "aws_apigatewayv2_route" "get_job_api_gateway_route" {
  api_id             = aws_apigatewayv2_api.frontend_api_gateway.id
  route_key          = "GET /${var.api_version}/getJob"
  target             = "integrations/${aws_apigatewayv2_integration.get_job_api_gateway_integration.id}"
  authorization_type = "AWS_IAM"
}

resource "aws_apigatewayv2_route" "create_job_api_gateway_route" {
  api_id             = aws_apigatewayv2_api.frontend_api_gateway.id
  route_key          = "POST /${var.api_version}/createJob"
  target             = "integrations/${aws_apigatewayv2_integration.create_job_api_gateway_integration.id}"
  authorization_type = "AWS_IAM"
}

#Define integrations
resource "aws_apigatewayv2_integration" "get_job_api_gateway_integration" {
  api_id             = aws_apigatewayv2_api.frontend_api_gateway.id
  integration_type   = "AWS_PROXY"
  integration_method = "POST"
  integration_uri = (var.get_job_lambda_provisioned_concurrency_enabled ? data.aws_lambda_function.get_job_lambda_versioned_data_source[0].invoke_arn
  : aws_lambda_function.get_job_lambda.invoke_arn)
}

resource "aws_apigatewayv2_integration" "create_job_api_gateway_integration" {
  api_id             = aws_apigatewayv2_api.frontend_api_gateway.id
  integration_type   = "AWS_PROXY"
  integration_method = "POST"
  integration_uri = (var.create_job_lambda_provisioned_concurrency_enabled ? data.aws_lambda_function.create_job_lambda_versioned_data_source[0].invoke_arn
  : aws_lambda_function.create_job_lambda.invoke_arn)
}

#Define perms
resource "aws_lambda_permission" "get_job_api_gateway_permission" {
  statement_id  = "AllowGetJobExecutionFromGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.get_job_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.frontend_api_gateway.execution_arn}/*"
  qualifier     = var.get_job_lambda_provisioned_concurrency_enabled ? aws_lambda_function.get_job_lambda.version : null
}

resource "aws_lambda_permission" "create_job_api_gateway_permission" {
  statement_id  = "AllowCreateJobExecutionFromGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.create_job_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.frontend_api_gateway.execution_arn}/*"
  qualifier     = var.create_job_lambda_provisioned_concurrency_enabled ? aws_lambda_function.create_job_lambda.version : null
}

#Define stage
resource "aws_apigatewayv2_stage" "frontend_api_gateway_stage" {
  api_id      = aws_apigatewayv2_api.frontend_api_gateway.id
  name        = var.api_env_stage_name
  auto_deploy = true

  lifecycle {
    create_before_destroy = true
  }

  default_route_settings {
    detailed_metrics_enabled = true
    logging_level            = "INFO"
    # Will be 0 if not defined. These are the max values
    throttling_burst_limit = 5000
    throttling_rate_limit  = 10000
  }

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.frontend_api_gateway_log_group.arn
    format = jsonencode({
      requestId       = "$context.requestId",
      requestTime     = "$context.requestTime",
      httpMethod      = "$context.httpMethod",
      routeKey        = "$context.routeKey",
      status          = "$context.status",
      responseLatency = "$context.responseLatency",
      responseLength  = "$context.responseLength"
    })
  }

  depends_on = [aws_cloudwatch_log_group.frontend_api_gateway_log_group]
}

# Use vended logs log group to store logs so resource policy does not run into size limits.
# https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/AWS-logs-and-resource-policy.html
resource "aws_cloudwatch_log_group" "frontend_api_gateway_log_group" {
  name              = "/aws/vendedlogs/apigateway/${var.frontend_api_name}"
  retention_in_days = var.frontend_cloudwatch_retention_days
}
