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

resource "google_spanner_instance" "spanner_instance" {
  name             = var.instance_name
  display_name     = var.instance_name
  config           = var.spanner_instance_config
  processing_units = var.spanner_processing_units

  labels = {
    environment = var.environment
  }
}

resource "google_spanner_database" "spanner_database" {
  instance = google_spanner_instance.spanner_instance.name
  name     = var.database_name
  ddl      = var.database_schema

  deletion_protection = var.spanner_database_deletion_protection
}
