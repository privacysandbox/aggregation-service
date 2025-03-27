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

################################################################################
# VPC Endpoints and Routes
################################################################################

data "aws_iam_policy_document" "dynamodb_vpce_policy" {
  statement {
    sid       = "AccessToDynamoDb"
    effect    = "Allow"
    resources = var.dynamodb_arns
    actions   = ["dynamodb:*"]
    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    # Note: The `aws:PrincipalArn` needs to match the principal that made
    # the request with the ARN that you specify in the policy. For IAM roles, the
    # request context returns the ARN of the role, not the ARN of the user that
    # assumed the role.  See https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_condition-keys.html#condition-keys-principalarn
    condition {
      test     = "StringLike"
      values   = var.dynamodb_allowed_principal_arns
      variable = "aws:PrincipalArn"
    }
  }
}

resource "aws_vpc_endpoint" "dynamodb_endpoint" {
  vpc_id       = aws_vpc.main.id
  service_name = "com.amazonaws.${local.region}.dynamodb"
  policy       = data.aws_iam_policy_document.dynamodb_vpce_policy.json

  tags = merge(
    {
      Name        = "${var.environment}-dynamodb-vpce"
      environment = var.environment
      subnet_type = "private"
    },
    local.vpc_default_tags
  )
}

data "aws_iam_policy_document" "s3_vpce_policy" {
  statement {
    sid    = "AccessToS3"
    effect = "Allow"
    # Permissive resource access because report bucket access is outside the
    # scope of operator infrastructure.  However, the bucket owner is encouraged
    # to restrict bucket access to the specific S3 endpoint of the operator.
    resources = ["*"]
    actions   = ["s3:*"]
    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    # Note: The `aws:PrincipalArn` needs to match the principal that made
    # the request with the ARN that you specify in the policy. For IAM roles, the
    # request context returns the ARN of the role, not the ARN of the user that
    # assumed the role.  See https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_condition-keys.html#condition-keys-principalarn
    condition {
      test     = "StringLike"
      values   = var.s3_allowed_principal_arns
      variable = "aws:PrincipalArn"
    }
  }
}

resource "aws_vpc_endpoint" "s3_endpoint" {
  vpc_id       = aws_vpc.main.id
  service_name = "com.amazonaws.${local.region}.s3"
  policy       = data.aws_iam_policy_document.s3_vpce_policy.json

  tags = merge(
    {
      Name        = "${var.environment}-s3-vpce"
      environment = var.environment
      subnet_type = "private"
    },
    local.vpc_default_tags
  )
}
