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

// s3 bucket to store Jars for the frontend lambdas
resource "aws_s3_bucket" "lambda_package_storage" {
  bucket_prefix = var.lambda_package_storage_bucket_prefix
  acl           = "private"

  tags = {
    name        = var.lambda_package_storage_bucket_prefix
    service     = "operator-service"
    environment = var.environment
    role        = "frontend-lambda-package-storage"
  }
}

resource "aws_s3_bucket_policy" "lambda_package_storage_bucket_deny_non_ssl_requests" {
  bucket = aws_s3_bucket.lambda_package_storage.id

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyNonSslRequests",
      "Action": "s3:*",
      "Effect": "Deny",
      "Resource": [
        "${aws_s3_bucket.lambda_package_storage.arn}",
        "${aws_s3_bucket.lambda_package_storage.arn}/*"
      ],
      "Condition": {
        "Bool": {
          "aws:SecureTransport": "false"
        }
      },
      "Principal": "*"
    }
  ]
}
EOF
}
