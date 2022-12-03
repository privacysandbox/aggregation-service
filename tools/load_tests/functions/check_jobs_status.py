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
"""Checks the status of the jobs triggered in the previous lambda.

Returns total, success and failed jobs counts and the job_request_id of the
failed jobs, if any.
"""
import base64
from datetime import datetime
import hashlib
import hmac
import json
import os
import sys
import time
import urllib3

ENDPOINT = "/stage/v1alpha/getJob"
FINISHED = "FINISHED"
SUCCESS_CODES = ("SUCCESS", "PRIVACY_BUDGET_ERROR")
MAX_RETRY_TIME = 60 * 5  # 5 mins
METHOD = "GET"

http = urllib3.PoolManager()


def validate_event(event):
    "Validates that required keys are present in the event."
    keys = [
        "access_key",
        "base_url",
        "host",
        "job_request_ids",
        "region",
        "secret_key",
        "service",
    ]
    for key in keys:
        if key not in event:
            return False
    return True


def sign(key, msg):
    return hmac.new(key, msg.encode("utf-8"), hashlib.sha256).digest()


def getSignatureKey(key, dateStamp, regionName, serviceName):
    kDate = sign(("AWS4" + key).encode("utf-8"), dateStamp)
    kRegion = sign(kDate, regionName)
    kService = sign(kRegion, serviceName)
    kSigning = sign(kService, "aws4_request")
    return kSigning


def create_headers(access_key, secret_key, host, region, service, job_request_id):
    t = datetime.utcnow()
    amz_date = t.strftime("%Y%m%dT%H%M%SZ")
    date_stamp = t.strftime("%Y%m%d")
    canonical_uri = ENDPOINT
    canonical_querystring = "job_request_id={job_request_id}".format(
        job_request_id=job_request_id
    )
    canonical_headers = "host:" + host + "\n" + "x-amz-date:" + amz_date + "\n"
    signed_headers = "host;x-amz-date"
    payload_hash = hashlib.sha256(("").encode("utf-8")).hexdigest()
    canonical_request = (
        METHOD
        + "\n"
        + canonical_uri
        + "\n"
        + canonical_querystring
        + "\n"
        + canonical_headers
        + "\n"
        + signed_headers
        + "\n"
        + payload_hash
    )
    algorithm = "AWS4-HMAC-SHA256"
    credential_scope = date_stamp + "/" + region + "/" + service + "/" + "aws4_request"
    string_to_sign = (
        algorithm
        + "\n"
        + amz_date
        + "\n"
        + credential_scope
        + "\n"
        + hashlib.sha256(canonical_request.encode("utf-8")).hexdigest()
    )
    signing_key = getSignatureKey(secret_key, date_stamp, region, service)
    signature = hmac.new(
        signing_key, (string_to_sign).encode("utf-8"), hashlib.sha256
    ).hexdigest()
    authorization_header = (
        algorithm
        + " "
        + "Credential="
        + access_key
        + "/"
        + credential_scope
        + ", "
        + "SignedHeaders="
        + signed_headers
        + ", "
        + "Signature="
        + signature
    )
    return {
        "X-Amz-Date": amz_date,
        "Authorization": authorization_header,
    }


def extract_from_response(data):
    "Extracts relevant data from the response."
    datetime_format = "%Y-%m-%dT%H:%M:%S.%fZ"
    received = datetime.strptime(data["request_received_at"], datetime_format)
    finished = datetime.strptime(data["request_updated_at"], datetime_format)
    job_status = data["job_status"]
    return_code = data.get("result_info", {}).get("return_code", "")
    error_summary = data.get("result_info", {}).get(
        "error_summary", {"error_counts": []}
    )
    error_count = 0
    for error in error_summary["error_counts"]:
        error_count += error["count"]
    return {
        "job_status": job_status,
        "return_code": return_code,
        "error_summary": error_summary,
        "error_count": error_count,
        "received": received,
        "finished": finished,
    }


def make_get_request(job_request_id, event):
    "Makes a GET request for a job_request_id."
    headers = create_headers(
        event["access_key"],
        event["secret_key"],
        event["host"],
        event["region"],
        event["service"],
        job_request_id,
    )
    url = "{base_url}{endpoint}?job_request_id={job_request_id}".format(
        base_url=event["base_url"], endpoint=ENDPOINT, job_request_id=job_request_id
    )
    response = http.request(
        METHOD,
        url,
        headers=headers,
    )
    return json.loads(response.data.decode("utf-8")), response.status


def check_job_status(job_request_id, event, received_at, finished_at):
    "Checks the status of a single job."
    start_time = time.time()
    status = False
    return_code = "MAX_RETRY_TIME_EXCEEDED"
    error_count = 0
    while time.time() - start_time < MAX_RETRY_TIME:
        resp, resp_status = make_get_request(job_request_id, event)

        # Error response code.
        if resp_status >= 400:
            print(resp)
            return_code = "ERROR"
            break

        data = extract_from_response(resp)
        return_code = data["return_code"]
        error_count = data["error_count"]

        # Job is still running.
        if data["job_status"] != FINISHED:
            print(resp)
            time.sleep(1)
            continue

        # Job is successful.
        if data["return_code"] in SUCCESS_CODES and data["error_count"] == 0:
            received_at.append(data["received"])
            finished_at.append(data["finished"])
            status = True

        break

    return {"status": status, "return_code": return_code, "error_count": error_count}


def check_jobs_status(event, failed_job_ids, received_at, finished_at):
    "Checks the job_status for the list of jobs."
    job_request_ids = event["job_request_ids"]
    for job_request_id in job_request_ids:
        overall_status = check_job_status(
            job_request_id, event, received_at, finished_at
        )
        if not overall_status["status"]:
            failed_job_ids.append(
                {
                    "job_request_id": job_request_id,
                    "return_code": overall_status["return_code"],
                    "error_count": overall_status["error_count"],
                }
            )


def calc_avg_completion_time(received_at, finished_at):
    "Calculates the averge time it takes for a job to complete."
    no_of_jobs = len(received_at)
    _sum = 0
    for idx in range(no_of_jobs):
        _sum += (finished_at[idx] - received_at[idx]).seconds
    return _sum / no_of_jobs


def lambda_handler(events, context):
    failed_job_ids = []
    received_at = []
    finished_at = []
    total = 0
    errot_count = 0
    average_time = None
    total_time = None
    for event in events:
        if not validate_event(event):
            raise Exception("Please provide access_key, secret_key, host and base_url")

        total += len(event["job_request_ids"])
        check_jobs_status(event, failed_job_ids, received_at, finished_at)
    failed = len(failed_job_ids)
    if finished_at and received_at:
        total_time = (max(finished_at) - min(received_at)).seconds
        average_time = calc_avg_completion_time(received_at, finished_at)
    return {
        "total_jobs": total,
        "success_jobs": total - failed,
        "failed_jobs": failed,
        "failed_job_details": failed_job_ids,
        "average_job_completion_time": average_time,
        "total_time": total_time,
    }
