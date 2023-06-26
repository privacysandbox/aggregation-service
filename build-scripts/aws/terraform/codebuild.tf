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

# Configure the AWS Provider
provider "aws" {
  region = var.region
}

# Get caller identy information
data "aws_caller_identity" "current" {}

# Read release version from VERSION file in <repository_root>
data "local_file" "version" {
  filename = "../../../VERSION"
}

locals {
  release_version = chomp(data.local_file.version.content)
}

# Create and manage S3 bucket for build artifacts
resource "aws_s3_bucket" "artifacts_output" {
  bucket        = var.build_artifacts_output_bucket
  force_destroy = var.force_destroy_build_artifacts_output_bucket
}

resource "aws_s3_bucket_acl" "artifacts_output" {
  bucket = aws_s3_bucket.artifacts_output.id
  acl    = "private"
  depends_on = [aws_s3_bucket_ownership_controls.artifacts_output_ownership_controls]
}

resource "aws_s3_bucket_ownership_controls" "artifacts_output_ownership_controls" {
  bucket = aws_s3_bucket.artifacts_output.id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

# Create and manage ECR Repository for build container
resource "aws_ecr_repository" "ecr_repository" {
  name = var.ecr_repo_name

  image_scanning_configuration {
    scan_on_push = true
  }
}

# Setup Github credentials
resource "aws_codebuild_source_credential" "example" {
  auth_type   = "PERSONAL_ACCESS_TOKEN"
  server_type = "GITHUB"
  token       = var.github_personal_access_token
}

# Setup codebuild role
resource "aws_iam_role" "codebuild_role" {
  name = "codebuild_role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "codebuild.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}

# Setup policy for codebuild role
resource "aws_iam_role_policy" "codebuild_policy" {
  role = aws_iam_role.codebuild_role.name

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Resource": [
        "*"
      ],
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:AttachVolume",
        "ec2:AuthorizeSecurityGroupIngress",
        "ec2:CopyImage",
        "ec2:CreateImage",
        "ec2:CreateKeypair",
        "ec2:CreateSecurityGroup",
        "ec2:CreateSnapshot",
        "ec2:CreateTags",
        "ec2:CreateVolume",
        "ec2:DeleteKeyPair",
        "ec2:DeleteSecurityGroup",
        "ec2:DeleteSnapshot",
        "ec2:DeleteVolume",
        "ec2:DeregisterImage",
        "ec2:DescribeImageAttribute",
        "ec2:DescribeImages",
        "ec2:DescribeInstances",
        "ec2:DescribeInstanceStatus",
        "ec2:DescribeRegions",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeSnapshots",
        "ec2:DescribeSubnets",
        "ec2:DescribeTags",
        "ec2:DescribeVolumes",
        "ec2:DetachVolume",
        "ec2:GetPasswordData",
        "ec2:ModifyImageAttribute",
        "ec2:ModifyInstanceAttribute",
        "ec2:ModifySnapshotAttribute",
        "ec2:RegisterImage",
        "ec2:RunInstances",
        "ec2:StopInstances",
        "ec2:TerminateInstances",
        "ec2:DescribeDhcpOptions",
        "ecr:CompleteLayerUpload",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart",
        "ecr:GetAuthorizationToken",
        "ecr:ListTagsForResource",
        "ecr:ListImages",
        "ecr:BatchGetImage",
        "ecr:DescribeImages",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutAnalyticsConfiguration",
        "s3:PutAccessPointConfigurationForObjectLambda",
        "s3:PutMetricsConfiguration",
        "s3:PutReplicationConfiguration",
        "s3:RestoreObject",
        "s3:PutObjectLegalHold",
        "s3:InitiateReplication",
        "s3:PutBucketCORS",
        "s3:PutInventoryConfiguration",
        "s3:ReplicateObject",
        "s3:PutObject",
        "s3:AbortMultipartUpload",
        "s3:PutObjectRetention",
        "s3:PutLifecycleConfiguration",
        "s3:UpdateJobPriority",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "${aws_s3_bucket.artifacts_output.arn}",
        "${aws_s3_bucket.artifacts_output.arn}/*",
        "arn:aws:s3:::codepipeline-${var.region}-*"
      ]
    }
  ]
}
POLICY
}

# Setup build project to build build container
resource "aws_codebuild_project" "bazel_build_container" {
  name          = "bazel-build-container"
  description   = "Build container for aggregation service build"
  build_timeout = "10"
  service_role  = aws_iam_role.codebuild_role.arn

  artifacts {
    type = "NO_ARTIFACTS"
  }

  environment {
    compute_type                = var.compute_type
    image                       = "aws/codebuild/standard:6.0"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode             = true

    environment_variable {
      name  = "AWS_DEFAULT_REGION"
      value = var.region
    }

    environment_variable {
      name  = "AWS_ACCOUNT_ID"
      value = data.aws_caller_identity.current.account_id
    }

    environment_variable {
      name  = "IMAGE_REPO_NAME"
      value = var.ecr_repo_name
    }

    environment_variable {
      name  = "IMAGE_TAG"
      value = local.release_version
    }
  }

  logs_config {
    s3_logs {
      status   = "ENABLED"
      location = "${aws_s3_bucket.artifacts_output.id}/build-log"
    }
  }

  source {
    type            = "GITHUB"
    location        = var.aggregation_service_github_repo
    git_clone_depth = 1

    buildspec = "build-scripts/aws/build-container/buildspec.yml"
  }

  source_version = var.aggregation_service_github_repo_branch == "" ? "v${local.release_version}" : var.aggregation_service_github_repo_branch
}


# Setup build project to build aggregation service AMI and jar artifacts
resource "aws_codebuild_project" "aggregation-service-artifacts-build" {
  name           = "aggregation-service-artifacts-build"
  description    = "Build aggregation service AMI and jar artifacts"
  build_timeout  = "60"
  queued_timeout = "60"

  service_role = aws_iam_role.codebuild_role.arn

  artifacts {
    type = "NO_ARTIFACTS"
  }

  cache {
    type     = "S3"
    location = "${aws_s3_bucket.artifacts_output.bucket}/build-cache"
  }

  environment {
    compute_type                = var.compute_type
    image                       = "${aws_ecr_repository.ecr_repository.repository_url}:${local.release_version}"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "SERVICE_ROLE"
    privileged_mode             = true

    environment_variable {
      name  = "AWS_DEFAULT_REGION"
      value = var.region
    }

    environment_variable {
      name  = "AWS_ACCOUNT_ID"
      value = data.aws_caller_identity.current.account_id
    }

    environment_variable {
      name  = "PACKER_GITHUB_API_TOKEN"
      value = var.github_personal_access_token
    }

    environment_variable {
      name  = "JARS_PUBLISH_BUCKET"
      value = var.build_artifacts_output_bucket
    }

    environment_variable {
      name  = "JARS_PUBLISH_BUCKET_PATH"
      value = "aggregation-service"
    }
  }

  source {
    type            = "GITHUB"
    location        = var.aggregation_service_github_repo
    git_clone_depth = 1

    buildspec = "build-scripts/aws/buildspec.yml"
  }

  source_version = var.aggregation_service_github_repo_branch == "" ? "v${local.release_version}" : var.aggregation_service_github_repo_branch
}
