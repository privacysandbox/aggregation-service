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
"""Triggers the job on the Aggregation Service.

This runs for the prescribed number of times per lambda as passed in the event
(default = 1).

Sleep time in between requests is defaulted to 10 unless overridden in the
event.
"""
import base64
import datetime
import hashlib
import hmac
import json
import os
import sys
import time
import urllib3
import uuid

BUCKET = "aggregation-service-load-testing"
CONTENT_TYPE = "application/json"
ENDPOINT = "/stage/v1alpha/createJob"
METHOD = "POST"


def sign(key, msg):
    return hmac.new(key, msg.encode("utf-8"), hashlib.sha256).digest()


def getSignatureKey(key, dateStamp, regionName, serviceName):
    kDate = sign(("AWS4" + key).encode("utf-8"), dateStamp)
    kRegion = sign(kDate, regionName)
    kService = sign(kRegion, serviceName)
    kSigning = sign(kService, "aws4_request")
    return kSigning


def create_headers(payload, access_key, secret_key, host, region, service):
    t = datetime.datetime.utcnow()
    amz_date = t.strftime("%Y%m%dT%H%M%SZ")
    date_stamp = t.strftime("%Y%m%d")
    canonical_uri = ENDPOINT
    canonical_querystring = ""
    payload_hash = hashlib.sha256(payload.encode("utf-8")).hexdigest()
    canonical_headers = (
        "host:"
        + host
        + "\n"
        + "x-amz-content-sha256:"
        + payload_hash
        + "\n"
        + "x-amz-date:"
        + amz_date
        + "\n"
    )
    signed_headers = "host;x-amz-content-sha256;x-amz-date"
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
        "X-Amz-Content-Sha256": payload_hash,
        "Content-Type": CONTENT_TYPE,
    }


def generate_payload(
    debug_run,
    attribution_report_to,
    input_data_blob_prefix,
    input_data_bucket_name,
    output_data_blob_prefix,
    output_data_bucket_name,
    output_domain_bucket_name,
    output_domain_blob_prefix,
):
    job_request_id = str(uuid.uuid4())
    output_prefix = "output-data/{job_request_id}/{output_data_blob_prefix}".format(
        job_request_id=job_request_id, output_data_blob_prefix=output_data_blob_prefix
    )
    return {
        "job_request_id": job_request_id,
        "input_data_blob_prefix": input_data_blob_prefix,
        "input_data_bucket_name": input_data_bucket_name,
        "output_data_blob_prefix": output_prefix,
        "output_data_bucket_name": output_data_bucket_name,
        "postback_url": "fizz.com/api/buzz",
        "job_parameters": {
            "attribution_report_to": attribution_report_to,
            "output_domain_blob_prefix": output_domain_blob_prefix,
            "output_domain_bucket_name": output_domain_bucket_name,
            "debug_run": debug_run,
        },
    }


def lambda_handler(event, context):
    access_key = event.get("access_key")
    base_url = event.get("base_url")
    host = event.get("host")
    num_requests = int(event.get("numRequests", "1"))
    region = event.get("region", "us-east-1")
    secret_key = event.get("secret_key")
    service = event.get("service", "execute-api")
    sleep_time = int(event.get("timeBetweenRequests", "10"))

    if not (access_key and secret_key and host and base_url):
        raise Exception("Please provide access_key, secret_key, host and base_url")
    debug_run = event.get("debug_run")
    attribution_report_to = event.get("attribution_report_to")
    input_data_blob_prefix = event.get("input_data_blob_prefix")
    input_data_bucket_name = event.get("input_data_bucket_name")
    output_data_blob_prefix = event.get("output_data_blob_prefix")
    output_data_bucket_name = event.get("output_data_bucket_name")
    output_domain_bucket_name = event.get("output_domain_bucket_name")
    output_domain_blob_prefix = event.get("output_domain_blob_prefix")

    url = "{base_url}{endpoint}".format(base_url=base_url, endpoint=ENDPOINT)
    job_request_ids = []
    http = urllib3.PoolManager()
    success = 0
    failed = 0
    for _ in range(num_requests):
        payload_dict = generate_payload(
            debug_run,
            attribution_report_to,
            input_data_blob_prefix,
            input_data_bucket_name,
            output_data_blob_prefix,
            output_data_bucket_name,
            output_domain_bucket_name,
            output_domain_blob_prefix,
        )

        payload = json.dumps(payload_dict)
        headers = create_headers(payload, access_key, secret_key, host, region, service)
        print(url)
        print(headers)
        print(payload)
        response = http.request(
            METHOD,
            url,
            headers=headers,
            body=payload,
        )
        if response.status >= 400:
            data = json.loads(response.data.decode("utf-8"))
            print(data)
            failed += 1
        else:
            success += 1
            job_request_ids.append(payload_dict["job_request_id"])
        time.sleep(sleep_time)
    return {
        "success": success,
        "failed": failed,
        "job_request_ids": job_request_ids,
        "base_url": base_url,
        "host": host,
        "region": region,
        "access_key": access_key,
        "secret_key": secret_key,
        "service": service,
    }
