# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Tests the data_generation lambda functions."""
import json

from unittest import TestCase
from unittest import main as unittest_main
from unittest.mock import patch

from trigger_jobs import lambda_handler as trigger_jobs_handler


class MockTriggerJobSuccessResponse:
  status = 202


class MockTriggerJobBadRequestResponse:
  status = 400
  data = json.dumps({"message": "bad request"}).encode("utf-8")


class MockTriggerJobServerErrorResponse:
  status = 500
  data = json.dumps({"message": "server error"}).encode("utf-8")


class TriggerJobsTests(TestCase):

  def test_trigger_jobs_no_access_key_raises_exception(self):
    with patch("urllib3.PoolManager.request") as resp:
      resp.return_value = MockTriggerJobSuccessResponse()
      event = {
          "base_url": "foo.bar",
          "numRequests": 3,
          "timeBetweenRequests": "0",
      }
      with self.assertRaises(Exception):
        result = trigger_jobs_handler(event, "")

  def test_trigger_jobs_successfully(self):
    with patch("urllib3.PoolManager.request") as resp:
      resp.return_value = MockTriggerJobSuccessResponse()
      event = {
          "base_url": "foo.bar",
          "numRequests": 3,
          "timeBetweenRequests": "0",
          "access_key": "key",
          "secret_key": "secret",
          "host": "host",
          "debug_run": "false",
          "attribution_report_to": "a",
          "input_data_blob_prefix": "a",
          "input_data_bucket_name": "a",
          "output_data_blob_prefix": "a",
          "output_data_bucket_name": "a",
          "output_domain_bucket_name": "a",
          "output_domain_blob_prefix": "a",
      }
      result = trigger_jobs_handler(event, "")
    self.assertEqual(result["success"], event["numRequests"])

  def test_trigger_jobs_with_bad_request(self):
    with patch("urllib3.PoolManager.request") as resp:
      resp.return_value = MockTriggerJobBadRequestResponse()
      event = {
          "base_url": "foo.bar",
          "numRequests": 2,
          "timeBetweenRequests": "0",
          "access_key": "key",
          "secret_key": "secret",
          "host": "host",
          "debug_run": "false",
          "attribution_report_to": "a",
          "input_data_blob_prefix": "a",
          "input_data_bucket_name": "a",
          "output_data_blob_prefix": "a",
          "output_data_bucket_name": "a",
          "output_domain_bucket_name": "a",
          "output_domain_blob_prefix": "a",
      }
      result = trigger_jobs_handler(event, "")
    self.assertEqual(result["failed"], event["numRequests"])

  def test_trigger_jobs_server_error(self):
    with patch("urllib3.PoolManager.request") as resp:
      resp.return_value = MockTriggerJobServerErrorResponse()
      event = {
          "base_url": "foo.bar",
          "numRequests": 2,
          "timeBetweenRequests": "0",
          "access_key": "key",
          "secret_key": "secret",
          "host": "host",
          "debug_run": "false",
          "attribution_report_to": "a",
          "input_data_blob_prefix": "a",
          "input_data_bucket_name": "a",
          "output_data_blob_prefix": "a",
          "output_data_bucket_name": "a",
          "output_domain_bucket_name": "a",
          "output_domain_blob_prefix": "a",
      }
      result = trigger_jobs_handler(event, "")
    self.assertEqual(result["failed"], event["numRequests"])


if __name__ == "__main__":
  unittest_main()
