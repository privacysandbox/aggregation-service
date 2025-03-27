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

resource "google_spanner_instance" "metadatadb_instance" {
  // shortened to jobmd suffix since max characters is 30
  name             = "${var.environment}-jobmd"
  display_name     = "${var.environment}-jobmd"
  config           = var.spanner_instance_config
  processing_units = var.spanner_processing_units

  labels = {
    environment = var.environment
  }
}

resource "google_spanner_database" "metadatadb" {
  instance = google_spanner_instance.metadatadb_instance.name
  name     = "jobmetadatadb"
  ddl = [
    <<-EOT
    CREATE TABLE JobMetadata (
      JobKey STRING(256) NOT NULL,
      RequestInfo JSON NOT NULL,
      JobStatus STRING(64) NOT NULL,
      ServerJobId STRING(50) NOT NULL,
      NumAttempts INT64 NOT NULL,
      RequestReceivedAt TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true),
      RequestUpdatedAt TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true),
      RequestProcessingStartedAt TIMESTAMP OPTIONS (allow_commit_timestamp=true),
      ResultInfo JSON,
      Ttl TIMESTAMP NOT NULL,
    )
    PRIMARY KEY (JobKey),
    ROW DELETION POLICY (OLDER_THAN(Ttl, INTERVAL 0 DAY))
    EOT
    ,
    <<-EOT
    CREATE TABLE AsgInstances (
      InstanceName STRING(256) NOT NULL,
      Status STRING(64) NOT NULL,
      RequestTime TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true),
      TerminationTime TIMESTAMP OPTIONS (allow_commit_timestamp=true),
      Ttl TIMESTAMP NOT NULL,
    )
    PRIMARY KEY (InstanceName),
    ROW DELETION POLICY (OLDER_THAN(Ttl, INTERVAL 0 DAY))
    EOT
    ,
    "CREATE INDEX AsgInstanceStatusIdx ON AsgInstances(Status)",
    "ALTER TABLE JobMetadata ALTER COLUMN RequestReceivedAt set OPTIONS (allow_commit_timestamp = false)",
    "ALTER TABLE JobMetadata ALTER COLUMN RequestUpdatedAt set OPTIONS (allow_commit_timestamp = false)",
    "ALTER TABLE JobMetadata ALTER COLUMN RequestProcessingStartedAt set OPTIONS (allow_commit_timestamp = false)",
    "ALTER TABLE AsgInstances ADD COLUMN TerminationReason STRING(64)"
  ]

  deletion_protection = var.spanner_database_deletion_protection
}
