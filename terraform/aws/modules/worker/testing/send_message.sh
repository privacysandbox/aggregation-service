#!/bin/bash
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

#
# Script to send messages to SQS Queue for testing.
# Make sure each message group id is unique since each subsequent item in the
# queue with the same message-group-id is blocked till the current message is
# processed in order to maintain FIFO ordering.
for i in {0..40}; do
  echo "message-$i"
  aws sqs send-message --queue-url https://sqs.us-east-1.amazonaws.com/985047324004/aveena-test.fifo --message-body "message-$i" --message-group-id "message-$i" --message-deduplication-id "message-$i"

done
