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
"""Tests the check_jobs_status lambda functions."""
import json

from unittest import TestCase
from unittest import main as unittest_main
from unittest.mock import patch

from check_jobs_status import lambda_handler as check_jobs_status_handler


class MockCheckJobStatusSuccessResponse:
    status = 200
    data = json.dumps(
        {
            "job_status": "FINISHED",
            "request_received_at": "2022-09-16T05:36:37.904Z",
            "request_updated_at": "2022-09-16T05:46:37.904Z",
            "result_info": {
                "return_code": "SUCCESS",
                "error_summary": {"error_counts": [{"count": 0}]},
            },
        }
    ).encode("utf-8")


class MockCheckJobWithSuccessReturnCodeAndErrorCountResponse:
    status = 200
    data = json.dumps(
        {
            "job_status": "FINISHED",
            "request_received_at": "2022-09-16T05:36:37.904Z",
            "request_updated_at": "2022-09-16T05:46:37.904Z",
            "result_info": {
                "return_code": "SUCCESS",
                "error_summary": {"error_counts": [{"count": 1000}]},
            },
        }
    ).encode("utf-8")


class MockCheckJobWithErrorReturnCodeResponse:
    status = 200
    data = json.dumps(
        {
            "job_status": "FINISHED",
            "request_received_at": "2022-09-16T05:36:37.904Z",
            "request_updated_at": "2022-09-16T05:46:37.904Z",
            "result_info": {
                "return_code": "UNSPECIFIED_ERROR",
                "error_summary": {"error_counts": [{"count": 0}]},
            },
        }
    ).encode("utf-8")


class MockCheckJobWithErrorCountResponse:
    status = 200
    data = json.dumps(
        {
            "job_status": "FINISHED",
            "request_received_at": "2022-09-16T05:36:37.904Z",
            "request_updated_at": "2022-09-16T05:46:37.904Z",
            "result_info": {
                "return_code": "UNSPECIFIED_ERROR",
                "error_summary": {"error_counts": [{"count": 1000}]},
            },
        }
    ).encode("utf-8")


class MockCheckJobStatusFailResponse:
    status = 400
    data = json.dumps({"error": "Something went wrong"}).encode("utf-8")


class CheckJobsStatusTests(TestCase):
    def test_check_job_status_no_access_key_raises_exception(self):
        with patch("urllib3.PoolManager.request") as resp:
            resp.return_value = MockCheckJobStatusSuccessResponse()
            event = [
                {
                    "job_request_ids": ["1", "2"],
                    "base_url": "foo.bar",
                },
                {
                    "job_request_ids": ["3", "4", "5"],
                    "base_url": "foo.bar",
                },
            ]
            with self.assertRaises(Exception):
                result = check_jobs_status_handler(event, "")

    def test_check_job_status_finished(self):
        with patch("urllib3.PoolManager.request") as resp:
            resp.return_value = MockCheckJobStatusSuccessResponse()
            event = [
                {
                    "job_request_ids": ["1", "2"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
                {
                    "job_request_ids": ["3", "4", "5"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
            ]
            result = check_jobs_status_handler(event, "")
        self.assertEqual(result["total_jobs"], 5)
        self.assertEqual(result["success_jobs"], 5)
        self.assertEqual(result["failed_jobs"], 0)
        self.assertEqual(result["average_job_completion_time"], 600)
        self.assertEqual(result["total_time"], 600)

    def test_check_job_status_failed(self):
        with patch("urllib3.PoolManager.request") as resp:
            resp.return_value = MockCheckJobStatusFailResponse()
            event = [
                {
                    "job_request_ids": ["1", "2"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
                {
                    "job_request_ids": ["3"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
            ]
            result = check_jobs_status_handler(event, "")
        self.assertEqual(result["total_jobs"], 3)
        self.assertEqual(result["success_jobs"], 0)
        self.assertEqual(result["failed_jobs"], 3)
        self.assertListEqual(
            result["failed_job_details"],
            [
                {"job_request_id": "1", "error_count": 0, "return_code": "ERROR"},
                {"job_request_id": "2", "error_count": 0, "return_code": "ERROR"},
                {
                    "job_request_id": "3",
                    "error_count": 0,
                    "return_code": "ERROR",
                },
            ],
        )

    def test_job_success_return_code_with_error_counts(self):
        with patch("urllib3.PoolManager.request") as resp:
            resp.return_value = MockCheckJobWithSuccessReturnCodeAndErrorCountResponse()
            event = [
                {
                    "job_request_ids": ["1", "2"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
                {
                    "job_request_ids": ["3"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
            ]
            result = check_jobs_status_handler(event, "")
        self.assertEqual(result["total_jobs"], 3)
        self.assertEqual(result["success_jobs"], 0)
        self.assertEqual(result["failed_jobs"], 3)
        self.assertListEqual(
            result["failed_job_details"],
            [
                {"job_request_id": "1", "error_count": 1000, "return_code": "SUCCESS"},
                {"job_request_id": "2", "error_count": 1000, "return_code": "SUCCESS"},
                {
                    "job_request_id": "3",
                    "error_count": 1000,
                    "return_code": "SUCCESS",
                },
            ],
        )

    def test_job_error_return_code_with_error_counts(self):
        with patch("urllib3.PoolManager.request") as resp:
            resp.return_value = MockCheckJobWithErrorCountResponse()
            event = [
                {
                    "job_request_ids": ["1", "2"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
                {
                    "job_request_ids": ["3"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
            ]
            result = check_jobs_status_handler(event, "")
        self.assertEqual(result["total_jobs"], 3)
        self.assertEqual(result["success_jobs"], 0)
        self.assertEqual(result["failed_jobs"], 3)
        self.assertListEqual(
            result["failed_job_details"],
            [
                {
                    "job_request_id": "1",
                    "error_count": 1000,
                    "return_code": "UNSPECIFIED_ERROR",
                },
                {
                    "job_request_id": "2",
                    "error_count": 1000,
                    "return_code": "UNSPECIFIED_ERROR",
                },
                {
                    "job_request_id": "3",
                    "error_count": 1000,
                    "return_code": "UNSPECIFIED_ERROR",
                },
            ],
        )

    def test_job_error_return_code_with_no_error_counts(self):
        with patch("urllib3.PoolManager.request") as resp:
            resp.return_value = MockCheckJobWithErrorReturnCodeResponse()
            event = [
                {
                    "job_request_ids": ["1", "2"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
                {
                    "job_request_ids": ["3"],
                    "base_url": "foo.bar",
                    "access_key": "access",
                    "secret_key": "secret",
                    "host": "host",
                    "region": "us-east-1",
                    "service": "execute-api",
                },
            ]
            result = check_jobs_status_handler(event, "")
        self.assertEqual(result["total_jobs"], 3)
        self.assertEqual(result["success_jobs"], 0)
        self.assertEqual(result["failed_jobs"], 3)
        self.assertListEqual(
            result["failed_job_details"],
            [
                {
                    "job_request_id": "1",
                    "error_count": 0,
                    "return_code": "UNSPECIFIED_ERROR",
                },
                {
                    "job_request_id": "2",
                    "error_count": 0,
                    "return_code": "UNSPECIFIED_ERROR",
                },
                {
                    "job_request_id": "3",
                    "error_count": 0,
                    "return_code": "UNSPECIFIED_ERROR",
                },
            ],
        )


if __name__ == "__main__":
    unittest_main()
