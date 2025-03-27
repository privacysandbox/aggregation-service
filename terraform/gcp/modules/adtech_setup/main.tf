/**
 * Copyright 2023 Google LLC
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
  deploy_service_account = var.deploy_service_account_email != "" ? var.deploy_service_account_email : google_service_account.deploy_service_account[0].email
  worker_service_account = var.worker_service_account_email != "" ? var.worker_service_account_email : google_service_account.worker_service_account[0].email
}

data "google_iam_policy" "policy_token_create" {
  binding {
    role    = "roles/iam.serviceAccountTokenCreator"
    members = var.service_account_token_creator_list
  }
}

resource "google_project_service" "artifactregistry" {
  project            = var.project
  service            = "artifactregistry.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "cloudresourcemanager" {
  project            = var.project
  service            = "cloudresourcemanager.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "spanner" {
  project            = var.project
  service            = "spanner.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "secretmanager" {
  project            = var.project
  service            = "secretmanager.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "compute" {
  project            = var.project
  service            = "compute.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "cloudfunctions" {
  project            = var.project
  service            = "cloudfunctions.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "run" {
  project            = var.project
  service            = "run.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "cloudbuild" {
  project            = var.project
  service            = "cloudbuild.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "cloudscheduler" {
  project            = var.project
  service            = "cloudscheduler.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "confidentialcomputing" {
  project            = var.project
  service            = "confidentialcomputing.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "iamcredentials" {
  project            = var.project
  service            = "iamcredentials.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "iam" {
  project            = var.project
  service            = "iam.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "containerscreening" {
  project            = var.project
  service            = "containerscanning.googleapis.com"
  disable_on_destroy = false
}

resource "google_service_account" "deploy_service_account" {
  count        = var.deploy_service_account_email == "" && var.deploy_service_account_name != "" ? 1 : 0
  project      = var.project
  account_id   = var.deploy_service_account_name
  display_name = "Deploy Service Account"
}

resource "google_service_account" "worker_service_account" {
  count        = var.worker_service_account_email == "" && var.worker_service_account_name != "" ? 1 : 0
  project      = var.project
  account_id   = var.worker_service_account_name
  display_name = "Worker Service Account"
}

resource "google_project_iam_custom_role" "deploy_custom_role" {
  project     = var.project
  role_id     = var.deploy_sa_role_name
  title       = "Deploy Custom Role"
  description = "Roles for deploying Aggregation Service"
  permissions = ["iam.serviceAccounts.getAccessToken", "storage.objects.list", "storage.objects.create", "storage.objects.get", "compute.networks.create", "monitoring.metricDescriptors.create", "compute.healthChecks.create", "secretmanager.secrets.create", "spanner.instances.create", "iam.serviceAccounts.create", "storage.buckets.create", "storage.objects.delete", "compute.globalOperations.get", "monitoring.metricDescriptors.get", "compute.healthChecks.get", "secretmanager.secrets.get", "spanner.instanceOperations.get", "iam.serviceAccounts.get", "storage.buckets.get", "monitoring.metricDescriptors.delete", "compute.healthChecks.delete", "secretmanager.secrets.delete", "iam.serviceAccounts.delete", "storage.buckets.delete", "secretmanager.versions.add", "secretmanager.versions.enable", "pubsub.topics.create", "secretmanager.versions.get", "secretmanager.versions.access", "secretmanager.versions.destroy", "pubsub.topics.get", "pubsub.topics.update", "pubsub.topics.attachSubscription", "pubsub.topics.delete", "pubsub.topics.detachSubscription", "pubsub.topics.list", "pubsub.topics.publish", "pubsub.topics.updateTag", "pubsub.subscriptions.create", "pubsub.subscriptions.delete", "pubsub.subscriptions.get", "pubsub.subscriptions.list", "pubsub.subscriptions.update", "pubsub.subscriptions.setIamPolicy", "pubsub.subscriptions.getIamPolicy", "pubsub.topics.setIamPolicy", "pubsub.topics.getIamPolicy", "compute.networks.get", "spanner.instances.get", "compute.routes.list", "spanner.databases.create", "spanner.databaseOperations.get", "compute.routes.delete", "compute.routes.create", "compute.instanceTemplates.create", "compute.firewalls.create", "spanner.databases.updateDdl", "compute.routers.create", "spanner.databases.get", "compute.networks.updatePolicy", "spanner.databases.getIamPolicy", "compute.instanceTemplates.get", "compute.networks.updatePolicy", "spanner.databases.getIamPolicy", "cloudfunctions.functions.create", "compute.routes.get", "cloudfunctions.functions.invoke", "run.jobs.run", "run.routes.invoke", "spanner.databases.setIamPolicy", "compute.firewalls.get", "spanner.databases.setIamPolicy", "compute.instanceGroupManagers.create", "compute.instanceTemplates.useReadOnly", "compute.instances.create", "compute.disks.create", "compute.subnetworks.use", "compute.instances.setMetadata", "compute.instances.setTags", "compute.routers.get", "compute.instanceTemplates.delete", "compute.routers.delete", "compute.firewalls.delete", "compute.instanceGroupManagers.get", "compute.routers.update", "compute.instances.setLabels", "spanner.databases.drop", "compute.networks.delete", "spanner.instances.delete", "compute.healthChecks.use", "iam.serviceAccounts.actAs", "iam.serviceAccounts.get", "iam.serviceAccounts.list", "resourcemanager.projects.get", "compute.autoscalers.create", "cloudfunctions.operations.get", "cloudfunctions.functions.get", "compute.instanceGroupManagers.use", "compute.instanceGroupManagers.use", "cloudfunctions.functions.delete", "compute.autoscalers.get", "compute.instanceGroups.use", "compute.healthChecks.useReadOnly", "compute.regionBackendServices.create", "monitoring.dashboards.create", "run.services.getIamPolicy", "cloudscheduler.jobs.create", "compute.autoscalers.get", "compute.regionBackendServices.get", "monitoring.dashboards.get", "run.services.setIamPolicy", "cloudscheduler.jobs.create", "compute.autoscalers.delete", "cloudscheduler.jobs.enable", "compute.regionBackendServices.delete", "monitoring.dashboards.delete", "cloudscheduler.jobs.get", "compute.instanceGroupManagers.delete", "cloudscheduler.jobs.delete", "compute.instanceGroups.delete", "cloudscheduler.jobs.delete", "compute.regionBackendServices.use", "compute.forwardingRules.create", "compute.forwardingRules.get", "compute.forwardingRules.delete", "artifactregistry.repositories.uploadArtifacts", "compute.instanceGroupManagers.update", "cloudfunctions.functions.update", "compute.autoscalers.update", "monitoring.notificationChannels.create", "monitoring.notificationChannels.delete", "monitoring.notificationChannels.get", "monitoring.alertPolicies.create", "monitoring.alertPolicies.delete", "monitoring.alertPolicies.get"]
}

resource "google_project_iam_custom_role" "worker_custom_role" {
  project     = var.project
  role_id     = var.worker_sa_role_name
  title       = "Worker Custom Role"
  description = "Roles for Aggregation Service worker"
  permissions = ["artifactregistry.repositories.downloadArtifacts", "artifactregistry.repositories.get", "artifactregistry.repositories.list", "artifactregistry.tags.get", "cloudnotifications.activities.list", "cloudtrace.insights.get", "cloudtrace.insights.list", "cloudtrace.stats.get", "cloudtrace.tasks.create", "cloudtrace.tasks.delete", "cloudtrace.tasks.get", "cloudtrace.tasks.list", "cloudtrace.traces.get", "cloudtrace.traces.list", "cloudtrace.traces.patch", "compute.acceleratorTypes.get", "compute.acceleratorTypes.list", "compute.addresses.create", "compute.addresses.delete", "compute.addresses.get", "compute.addresses.list", "compute.addresses.use", "compute.autoscalers.get", "compute.autoscalers.list", "compute.backendBuckets.addSignedUrlKey", "compute.backendBuckets.create", "compute.backendBuckets.delete", "compute.backendBuckets.deleteSignedUrlKey", "compute.backendBuckets.get", "compute.backendBuckets.list", "compute.backendBuckets.setSecurityPolicy", "compute.backendBuckets.update", "compute.backendServices.addSignedUrlKey", "compute.backendServices.create", "compute.backendServices.createTagBinding", "compute.backendServices.delete", "compute.backendServices.deleteSignedUrlKey", "compute.backendServices.get", "compute.backendServices.list", "compute.backendServices.setSecurityPolicy", "compute.backendServices.update", "compute.backendServices.use", "compute.externalVpnGateways.delete", "compute.externalVpnGateways.use", "compute.firewallPolicies.use", "compute.firewalls.get", "compute.firewalls.list", "compute.forwardingRules.create", "compute.forwardingRules.delete", "compute.forwardingRules.get", "compute.forwardingRules.list", "compute.forwardingRules.pscDelete", "compute.forwardingRules.pscSetLabels", "compute.forwardingRules.pscSetTarget", "compute.forwardingRules.pscUpdate", "compute.forwardingRules.setTarget", "compute.forwardingRules.update", "compute.forwardingRules.use", "compute.globalAddresses.create", "compute.globalAddresses.createInternal", "compute.globalAddresses.delete", "compute.globalAddresses.deleteInternal", "compute.globalAddresses.get", "compute.globalAddresses.list", "compute.globalAddresses.use", "compute.globalForwardingRules.delete", "compute.globalForwardingRules.get", "compute.globalForwardingRules.list", "compute.globalForwardingRules.pscDelete", "compute.globalForwardingRules.pscGet", "compute.globalForwardingRules.pscSetLabels", "compute.globalForwardingRules.pscSetTarget", "compute.globalForwardingRules.pscUpdate", "compute.globalForwardingRules.update", "compute.globalNetworkEndpointGroups.get", "compute.globalNetworkEndpointGroups.list", "compute.globalNetworkEndpointGroups.use", "compute.globalOperations.get", "compute.globalOperations.list", "compute.globalPublicDelegatedPrefixes.delete", "compute.globalPublicDelegatedPrefixes.get", "compute.globalPublicDelegatedPrefixes.list", "compute.globalPublicDelegatedPrefixes.update", "compute.globalPublicDelegatedPrefixes.updatePolicy", "compute.healthChecks.create", "compute.healthChecks.delete", "compute.healthChecks.get", "compute.healthChecks.list", "compute.healthChecks.update", "compute.healthChecks.useReadOnly", "compute.httpHealthChecks.create", "compute.httpHealthChecks.delete", "compute.httpHealthChecks.get", "compute.httpHealthChecks.list", "compute.httpHealthChecks.update", "compute.httpHealthChecks.useReadOnly", "compute.httpsHealthChecks.create", "compute.httpsHealthChecks.delete", "compute.httpsHealthChecks.get", "compute.httpsHealthChecks.list", "compute.httpsHealthChecks.update", "compute.httpsHealthChecks.useReadOnly", "compute.instanceGroupManagers.get", "compute.instanceGroupManagers.list", "compute.instanceGroupManagers.update", "compute.instanceGroupManagers.use", "compute.instanceGroups.get", "compute.instanceGroups.list", "compute.instanceGroups.update", "compute.instanceGroups.use", "compute.instanceSettings.get", "compute.instances.get", "compute.instances.getScreenshot", "compute.instances.getSerialPortOutput", "compute.instances.list", "compute.instances.listReferrers", "compute.instances.updateSecurity", "compute.instances.use", "compute.instances.useReadOnly", "compute.interconnectRemoteLocations.get", "compute.interconnectRemoteLocations.list", "compute.machineTypes.get", "compute.machineTypes.list", "compute.networkAttachments.create", "compute.networkAttachments.delete", "compute.networkAttachments.get", "compute.networkAttachments.list", "compute.networkEndpointGroups.get", "compute.networkEndpointGroups.list", "compute.networkEndpointGroups.use", "compute.networks.access", "compute.networks.get", "compute.networks.getEffectiveFirewalls", "compute.networks.getRegionEffectiveFirewalls", "compute.networks.list", "compute.networks.listPeeringRoutes", "compute.networks.mirror", "compute.networks.setFirewallPolicy", "compute.networks.updatePeering", "compute.networks.updatePolicy", "compute.networks.use", "compute.networks.useExternalIp", "compute.packetMirrorings.get", "compute.packetMirrorings.list", "compute.projects.get", "compute.regionUrlMaps.use", "compute.regionUrlMaps.validate", "compute.regions.get", "compute.regions.list", "compute.routers.create", "compute.routers.delete", "compute.routers.get", "compute.routers.list", "compute.routers.update", "compute.routers.use", "compute.routes.create", "compute.routes.get", "compute.routes.list", "compute.serviceAttachments.create", "compute.serviceAttachments.delete", "compute.serviceAttachments.get", "compute.serviceAttachments.list", "compute.serviceAttachments.update", "compute.serviceAttachments.use", "compute.sslCertificates.get", "compute.sslCertificates.list", "compute.subnetworks.create", "compute.subnetworks.delete", "compute.subnetworks.expandIpCidrRange", "compute.subnetworks.get", "compute.subnetworks.list", "compute.subnetworks.mirror", "compute.subnetworks.setPrivateIpGoogleAccess", "compute.subnetworks.update", "compute.subnetworks.use", "compute.subnetworks.useExternalIp", "compute.targetGrpcProxies.create", "compute.targetGrpcProxies.delete", "compute.targetGrpcProxies.get", "compute.targetGrpcProxies.list", "compute.targetGrpcProxies.update", "compute.targetGrpcProxies.use", "compute.targetHttpProxies.create", "compute.targetHttpProxies.delete", "compute.targetHttpProxies.get", "compute.targetHttpProxies.list", "compute.targetHttpProxies.setUrlMap", "compute.targetHttpsProxies.setCertificateMap", "compute.targetHttpsProxies.setQuicOverride", "compute.targetHttpsProxies.setSslCertificates", "compute.targetHttpsProxies.setUrlMap", "compute.targetHttpsProxies.update", "compute.targetHttpsProxies.use", "compute.targetInstances.create", "compute.targetInstances.delete", "compute.targetInstances.get", "compute.targetInstances.list", "compute.targetInstances.setSecurityPolicy", "compute.targetInstances.use", "compute.targetPools.addHealthCheck", "compute.targetPools.addInstance", "compute.targetPools.create", "compute.targetPools.delete", "compute.targetPools.get", "compute.targetPools.list", "compute.targetPools.removeHealthCheck", "compute.targetPools.removeInstance", "compute.targetPools.setSecurityPolicy", "compute.targetPools.update", "compute.targetPools.use", "compute.targetSslProxies.delete", "compute.targetSslProxies.get", "compute.targetSslProxies.list", "compute.targetSslProxies.setBackendService", "compute.targetSslProxies.setCertificateMap", "compute.targetSslProxies.setProxyHeader", "compute.targetSslProxies.setSslCertificates", "compute.targetSslProxies.setSslPolicy", "compute.targetSslProxies.update", "compute.targetSslProxies.use", "compute.targetTcpProxies.create", "compute.targetTcpProxies.delete", "compute.targetTcpProxies.get", "compute.targetTcpProxies.list", "compute.targetTcpProxies.update", "compute.targetTcpProxies.use", "compute.vpnGateways.use", "confidentialcomputing.challenges.create", "confidentialcomputing.challenges.verify", "confidentialcomputing.locations.get", "confidentialcomputing.locations.list", "iam.serviceAccounts.getAccessToken", "logging.logEntries.create", "logging.logEntries.route", "monitoring.alertPolicies.delete", "monitoring.alertPolicies.get", "monitoring.alertPolicies.list", "monitoring.alertPolicies.update", "monitoring.dashboards.create", "monitoring.dashboards.delete", "monitoring.dashboards.get", "monitoring.dashboards.list", "monitoring.dashboards.update", "monitoring.groups.delete", "monitoring.groups.get", "monitoring.groups.list", "monitoring.groups.update", "monitoring.metricDescriptors.create", "monitoring.metricDescriptors.delete", "monitoring.metricDescriptors.get", "monitoring.metricDescriptors.list", "monitoring.monitoredResourceDescriptors.get", "monitoring.monitoredResourceDescriptors.list", "monitoring.publicWidgets.create", "monitoring.publicWidgets.delete", "monitoring.publicWidgets.get", "monitoring.publicWidgets.list", "monitoring.publicWidgets.update", "monitoring.services.create", "monitoring.services.delete", "monitoring.services.get", "monitoring.services.list", "monitoring.services.update", "monitoring.slos.create", "monitoring.slos.delete", "monitoring.slos.get", "monitoring.slos.list", "monitoring.slos.update", "monitoring.snoozes.create", "monitoring.snoozes.get", "monitoring.snoozes.list", "monitoring.snoozes.update", "monitoring.timeSeries.create", "monitoring.timeSeries.list", "monitoring.uptimeCheckConfigs.create", "monitoring.uptimeCheckConfigs.delete", "monitoring.uptimeCheckConfigs.get", "monitoring.uptimeCheckConfigs.list", "monitoring.uptimeCheckConfigs.update", "pubsub.topics.publish", "resourcemanager.projects.get", "secretmanager.versions.access", "servicenetworking.operations.get", "serviceusage.services.enable", "stackdriver.resourceMetadata.list", "stackdriver.resourceMetadata.write", "storage.buckets.get", "storage.buckets.getObjectInsights", "storage.buckets.list", "storage.buckets.update", "storage.managedFolders.delete", "storage.managedFolders.get", "storage.managedFolders.list", "storage.multipartUploads.abort", "storage.multipartUploads.create", "storage.multipartUploads.list", "storage.multipartUploads.listParts", "storage.objects.create", "storage.objects.delete", "storage.objects.get", "storage.objects.list", "storage.objects.update"]
}

resource "google_project_iam_member" "deploy_service_account_role" {
  role    = "projects/${var.project}/roles/${google_project_iam_custom_role.deploy_custom_role.role_id}"
  member  = "serviceAccount:${local.deploy_service_account}"
  project = var.project
}

resource "google_project_iam_member" "worker_service_account_role" {
  role    = "projects/${var.project}/roles/${google_project_iam_custom_role.worker_custom_role.role_id}"
  member  = "serviceAccount:${local.worker_service_account}"
  project = var.project
}

resource "google_service_account_iam_policy" "deploy_token_creator_policy" {
  service_account_id = "projects/${var.project}/serviceAccounts/${local.deploy_service_account}"
  policy_data        = data.google_iam_policy.policy_token_create.policy_data
}

resource "google_artifact_registry_repository" "artifact_repo" {
  count         = var.artifact_repo_name != "" ? 1 : 0
  project       = var.project
  location      = var.artifact_repo_location
  repository_id = var.artifact_repo_name
  description   = "Adtech Services image repository"
  format        = "DOCKER"
}

resource "google_storage_bucket" "data_bucket" {
  count    = var.data_bucket_name != "" ? 1 : 0
  project  = var.project
  name     = var.data_bucket_name
  location = var.data_bucket_location

  versioning {
    enabled = var.data_bucket_versioning
  }

  public_access_prevention    = "enforced"
  uniform_bucket_level_access = true
}
