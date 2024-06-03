# Copyright 2023 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# bookworm-slim stable
ARG  DEBIAN_VERSION
FROM debian@${DEBIAN_VERSION}

RUN \
    # This makes add-apt-repository available.
    apt-get update && \
    apt-get -y upgrade && \
    apt-get -y install \
        2to3 \
        curl \
        git \
        gnupg \
        npm \
        python-is-python3 \
        python3 \
        python3-pkg-resources \
        software-properties-common \
        tree \
        unzip

RUN \
    # Install OpenJDK
    apt-get -y install openjdk-17-jdk
RUN \
    # Install bazel
    npm install -g @bazel/bazelisk

# symlink bazel to bazelisk binary
RUN ln -s bazelisk /usr/bin/bazel

RUN \
    # Install Docker (https://docs.docker.com/engine/install/debian/)
    apt-get -y install \
        apt-transport-https \
        ca-certificates \
        lsb-release
RUN \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce docker-ce-cli

RUN \
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.asc] https://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | tee /usr/share/keyrings/cloud.google.asc && \
    apt-get update && apt-get -y install google-cloud-cli && \
    apt-get -y autoclean && apt-get -y autoremove

# Store the Bazel outputs under /workspace so that the symlinks under bazel-bin (et al) are accessible
# to downstream build steps.
RUN mkdir -p /workspace
RUN echo 'startup --output_base=/workspace/.bazel' > ~/.bazelrc

ENTRYPOINT ["bazel"]
