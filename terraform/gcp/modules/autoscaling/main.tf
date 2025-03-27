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
locals {
  # A hack to trigger replacement on creation of
  # worker_template_mig_replace_trigger. Per https://github.com/hashicorp/terraform/issues/31685#issuecomment-1230349751,
  # replace_triggered_by does not work on initial creation of the trigger
  # null_resource.
  #
  # Reformatted here from a potentially long string of integer value to a hex
  # that's 1-6 characters long.
  worker_instance_group_suffix = format("%x", parseint(substr(null_resource.worker_template_mig_replace_trigger.id, 0, 7), 10))
}

resource "null_resource" "worker_template_mig_replace_trigger" {
  triggers = {
    # To work around the google_compute_region_instance_group_manager update
    # error "Networks specified in new and old network interfaces must be the
    # same." when network is changed in the template. This creates a resource
    # trigger for replacement.
    #
    # Using an express that considers the complete network_interface list does
    # not seem to work and always forces replacement, perhaps due to some non-
    # determinism of the list. Direct indexing into the first element works as
    # desired.
    network = length(var.worker_template.network_interface) > 0 ? var.worker_template.network_interface[0].network : ""
  }
}

resource "google_compute_region_instance_group_manager" "worker_instance_group" {
  name               = "${var.environment}-worker-mig-${local.worker_instance_group_suffix}"
  description        = "The managed instance group for SCP worker instances."
  base_instance_name = "${var.environment}-worker"

  region = var.region

  version {
    instance_template = var.worker_template.id
  }

  # TODO: Update with dynamic rolling update policy
  update_policy {
    minimal_action               = "REPLACE"
    type                         = "OPPORTUNISTIC"
    instance_redistribution_type = "PROACTIVE"
    max_unavailable_fixed        = 5
  }

  lifecycle {
    replace_triggered_by = [
      null_resource.worker_template_mig_replace_trigger
    ]
  }
}

resource "google_compute_region_autoscaler" "worker_autoscaler" {
  provider = google-beta

  name    = "${var.environment}-worker-autoscaler"
  project = var.project_id
  region  = var.region
  target  = google_compute_region_instance_group_manager.worker_instance_group.id

  autoscaling_policy {
    max_replicas    = var.max_worker_instances
    min_replicas    = var.min_worker_instances
    cooldown_period = 60
    # Scale down is managed by custom solution
    mode = "ONLY_UP"

    metric {
      name                       = "pubsub.googleapis.com/subscription/num_undelivered_messages"
      filter                     = "resource.type = pubsub_subscription AND resource.label.subscription_id = ${var.jobqueue_subscription_name}"
      single_instance_assignment = var.autoscaling_jobs_per_instance
    }
  }

  # Required otherwise worker_instance_group hits resourceInUseByAnotherResource error when replacing
  lifecycle {
    replace_triggered_by = [google_compute_region_instance_group_manager.worker_instance_group.id]
  }
}
