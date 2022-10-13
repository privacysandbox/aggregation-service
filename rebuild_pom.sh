#!/usr/bin/env bash
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
# Generates or regenerates a pom.xml file in the current folder based on the
# Maven remote dependencies in the //java/... folder.
#
# While this project does not use Maven as its build tool, some developer
# tooling (e.g. language servers) currently have better support for Maven-style
# projects than Bazel-style projects and the generation of a pom.xml should help
# those tools locate project dependencies. Does not generate valid build rules.
set -eu

# Bazel query to return direct jvm_import dependencies in the java directory.
QUERY="kind(\"jvm_import\", let project = //java/...:* in deps(\$project, 1) except \$project)"
# Grep filter for the spec build rule lines we want from bazel query output.
FILTER='jars = \["@maven//:v1/https/repo1.maven.org/maven2/'

# Example input string:
#  jars = ["@maven//:v1/https/repo1.maven.org/maven2/com/google/guava/guava/30.1.1-jre/guava-30.1.1-jre.jar"],
# Converts the above input line into a proper maven <dependency/> XML stanza
TO_XML_STANZA=$(cat <<-"EOF"
# Strip file name, extract version + remainder
s#maven2/(.+)/([^/]+)\/[^/]+\.jar.+##;
$version = $2;
$s = $1;

# Split remainder into group + artifact
$s =~ s#(.+)/([^/]+)$##;
$group = $1;
$artifact = $2;

$group =~ s#/#.#g;

print "<dependency><artifactId>$artifact</artifactId><groupId>$group</groupId><version>$version</version></dependency>\n"
EOF
           )

DEPS=$(bazel query "$QUERY" --output=build | grep "$FILTER" | perl -ne "$TO_XML_STANZA")

OUT=$(cat <<-EOF
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.mycompany.app</groupId>
  <artifactId>my-app</artifactId>
  <version>1.0-SNAPSHOT</version>

  <build>
    <sourceDirectory>\${project.basedir}/java</sourceDirectory>
    <testSourceDirectory>\${project.basedir}/javatests</testSourceDirectory>
    <plugins>
      <plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-compiler-plugin</artifactId>
	<version>3.8.0</version>
	<configuration>
	  <release>11</release>
	</configuration>
      </plugin>
    </plugins>
  </build>

  <dependencies>
      ${DEPS}
  </dependencies>
</project>
EOF
)

echo $OUT | xmllint --format - > pom.xml
