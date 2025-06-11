// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

packer {
  required_plugins {
    amazon = {
      version = ">= 1.2.1"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

locals {
  //AMI naming does not support some special characters
  timestamp = formatdate("YYYY-MM-DD'T'hh-mm-ssZ", timestamp())

  # List of additional systemd configuration to add to aggregate-worker.
  aggregate_worker_overrides = [
    {enable_worker_debug_mode} ? "Environment=ENABLE_WORKER_DEBUG_MODE=1" : "",
    "TimeoutStartSec=600", # Extend start-up timeout for large memory Enclave.
  ]
}

source "amazon-ebs" "sample-ami" {
  ami_name      = "{ami_name}--${local.timestamp}"
  instance_type = "{ec2_instance}"
  region        = "{aws_region}"
  ami_groups    = {ami_groups}
  subnet_id     = "{subnet_id}"

  // Custom Base AMI
  source_ami_filter {
    filters = {
      name                = "al2023-ami-minimal-*-x86_64"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners = [
      "amazon"
    ]
  }
  launch_block_device_mappings {
    device_name = "/dev/xvda"
    volume_size = 8
    delete_on_termination = true
  }
  ssh_username = "ec2-user"
  ssh_timeout = "15m"
  temporary_key_pair_type = "ed25519"
  # enforces imdsv2 support on the running instance being provisioned by Packer
  metadata_options {
    http_endpoint = "enabled"
    http_tokens = "required"
    http_put_response_hop_limit = 1
  }
  imds_support  = "v2.0" # enforces imdsv2 support on the resulting AMI
}

build {
  sources = [
    "source.amazon-ebs.sample-ami"
  ]

  provisioner "file" {
    source = "{licenses}"
    destination = "/tmp/licenses.tar"
  }

  provisioner "file" {
    source      = "{container_path}"
    destination = "/tmp/{container_filename}"
  }

  provisioner "shell" {
    inline = ["mkdir /tmp/rpms"]
  }

  provisioner "file" {
    sources     = {rpms}
    destination = "/tmp/rpms/"
  }

  provisioner "file" {
    source      = "{enclave_allocator}"
    destination = "/tmp/allocator.yaml"
  }

  provisioner "shell" {
    script = "{provision_script}"
  }

  # Populate worker overrides file.
  provisioner "file" {
    content = <<-EOF
    [Service]
    ${join("\n", local.aggregate_worker_overrides)}
    EOF
    destination = "/tmp/aggregate-worker_override.conf"
  }

  # Create worker overrides directory and move override there;
  provisioner "shell" {
    inline = [
      "sudo mkdir /etc/systemd/system/aggregate-worker.service.d/",
      "sudo mv /tmp/aggregate-worker_override.conf /etc/systemd/system/aggregate-worker.service.d/override.conf",
      # Clean up files used by provision_script
      "sudo rm -rf /tmp/rpms",
      "sudo rm -f /tmp/{container_filename}",
      "sudo rm -f /tmp/allocator.yaml",
      "sudo rm -f /tmp/licenses.tar",
    ]
  }

  provisioner "shell" {
    # This should be the last step because it removes access from Packer.

    environment_vars = [
      "UNINSTALL_SSH_SERVER={uninstall_ssh_server}",
    ]

    inline = [
      # Standard AMI cleanup per:
      # https://aws.amazon.com/articles/how-to-share-and-use-public-amis-in-a-secure-manner/
      # This removes the auto-generated Packer key (created in
      # /home/ec2-user/.ssh/authorized_keys).
      "sudo find / -name authorized_keys -delete -print",
      # Note: omitting deletion of shell history and VCS files because they are
      # not present in these AMIs.
      "if ( $UNINSTALL_SSH_SERVER ); then sudo rpm -e openssh-server; fi",
    ]
  }
}
