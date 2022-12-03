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
from unittest import TestCase
from unittest import main as unittest_main

from data_generation import lambda_handler as data_generation_handler


class DataGenerationTests(TestCase):
    def test_data_generation_defaults(self):
        self.assertEqual(len(data_generation_handler({}, "")), 1)

    def test_data_generation(self):
        num_workers = 3
        self.assertEqual(
            len(data_generation_handler({"numWorkers": num_workers}, "")), num_workers
        )


if __name__ == "__main__":
    unittest_main()
