# Dependencies and Licenses

The deployment of the Amazon Web Services [Nitro Enclaves](https://aws.amazon.com/ec2/nitro/nitro-enclaves/) based Aggregation Service depends on several packaged
artifacts listed below.
These artifacts can be downloaded with the [download-dependencies.sh](./terraform/aws/download-dependencies.sh)
script.

## Packaged AWS Lambda Jars

### AwsChangeHandlerLambda_{version}.jar

| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| aopalliance | aopalliance | 1.0 |Public Domain | N/A |
| com.amazonaws|aws-lambda-java-core|1.2.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events-sdk-transformer|3.1.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events|3.8.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.core|jackson-core|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.core|jackson-databind|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.android|annotations|4.1.1.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.service|auto-service-annotations|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.service|auto-service|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.value|auto-value-annotations|1.7.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.value|auto-value|1.7.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto|auto-common|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.errorprone|error_prone_annotations|2.0.15|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|failureaccess|1.0.1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|guava|30.1-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.inject|guice|4.2.3|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.j2objc|j2objc-annotations|1.3|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.typesafe.netty|netty-reactive-streams-http|2.0.5|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.typesafe.netty|netty-reactive-streams|2.0.5|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| commons-codec|commons-codec|1.15|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0.txt> |
| commons-logging|commons-logging|1.1.1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| io.netty|netty-buffer|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-codec-http|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-common|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-epoll|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| javax.inject|javax.inject|1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| joda-time|joda-time|2.6|Apache 2 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.apache.httpcomponents|httpclient|4.5.13|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.apache.httpcomponents|httpcore|4.4.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.checkerframework|checker-qual|3.8.0|The MIT License | <http://opensource.org/licenses/MIT> |
| org.json|json|20180813|The JSON License | <http://json.org/license.html> |
| org.reactivestreams|reactive-streams|1.0.3 | CC0 | <http://creativecommons.org/publicdomain/zero/1.0/> |
| org.slf4j|slf4j-api|1.7.30|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|arns|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-xml-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|connect|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|kms|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|lambda|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|pi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|ram|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|s3|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |

### aws_apigateway_frontend_{version}.jar

| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| aopalliance | aopalliance | 1.0 |Public Domain | N/A |
| com.amazonaws|aws-lambda-java-core|1.2.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events-sdk-transformer|3.1.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events|3.8.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.core|jackson-core|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.core|jackson-databind|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.android|annotations|4.1.1.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.service|auto-service-annotations|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.service|auto-service|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.value|auto-value-annotations|1.7.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.value|auto-value|1.7.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto|auto-common|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.errorprone|error_prone_annotations|2.0.15|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|failureaccess|1.0.1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|guava|30.1-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.inject|guice|4.2.3|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.j2objc|j2objc-annotations|1.3|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.typesafe.netty|netty-reactive-streams-http|2.0.5|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.typesafe.netty|netty-reactive-streams|2.0.5|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| commons-codec|commons-codec|1.15|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0.txt> |
| commons-logging|commons-logging|1.1.1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| io.netty|netty-buffer|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-codec-http|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-common|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-epoll|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| javax.inject|javax.inject|1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| joda-time|joda-time|2.6|Apache 2 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.apache.httpcomponents|httpclient|4.5.13|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.apache.httpcomponents|httpcore|4.4.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.checkerframework|checker-qual|3.8.0|The MIT License | <http://opensource.org/licenses/MIT> |
| org.json|json|20180813|The JSON License | <http://json.org/license.html> |
| org.reactivestreams|reactive-streams|1.0.3 | CC0 | <http://creativecommons.org/publicdomain/zero/1.0/> |
| org.slf4j|slf4j-api|1.7.30|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|arns|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-xml-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|connect|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|kms|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|lambda|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|pi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|ram|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|s3|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |

### AwsFrontendCleanupLambda_{version}.jar

| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| aopalliance | aopalliance | 1.0 |Public Domain | N/A |
| com.amazonaws|aws-lambda-java-core|1.2.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events-sdk-transformer|3.1.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events|3.8.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.core|jackson-core|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.core|jackson-databind|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.12.2|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.android|annotations|4.1.1.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.service|auto-service-annotations|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.service|auto-service|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.value|auto-value-annotations|1.7.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto.value|auto-value|1.7.4|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.auto|auto-common|1|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.errorprone|error_prone_annotations|2.0.15|Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|failureaccess|1.0.1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|guava|30.1-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.inject|guice|4.2.3|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.google.j2objc|j2objc-annotations|1.3|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.typesafe.netty|netty-reactive-streams-http|2.0.5|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| com.typesafe.netty|netty-reactive-streams|2.0.5|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| commons-codec|commons-codec|1.15|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0.txt> |
| commons-logging|commons-logging|1.1.1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| io.netty|netty-buffer|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-codec-http|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-common|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-epoll|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.63.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport|4.1.53.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| javax.inject|javax.inject|1|The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| joda-time|joda-time|2.6|Apache 2 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.apache.httpcomponents|httpclient|4.5.13|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.apache.httpcomponents|httpcore|4.4.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |.txt |
| org.checkerframework|checker-qual|3.8.0|The MIT License | <http://opensource.org/licenses/MIT> |
| org.json|json|20180813|The JSON License | <http://json.org/license.html> |
| org.reactivestreams|reactive-streams|1.0.3 | CC0 | <http://creativecommons.org/publicdomain/zero/1.0/> |
| org.slf4j|slf4j-api|1.7.30|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|arns|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-xml-protocol|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|connect|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|kms|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|lambda|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|pi|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|ram|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|s3|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.16.104|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |

### AsgCapacityHandlerLambda_{version}.jar

| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| aopalliance | aopalliance | 1 | Public Domain |
| com.amazonaws | aws-lambda-java-core | 1.2.1 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws | aws-lambda-java-events | 3.8.0 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core | jackson-annotations | 2.12.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.fasterxml.jackson.core | jackson-core | 2.12.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.fasterxml.jackson.core | jackson-databind | 2.12.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.android | annotations | 4.1.1.4 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value | auto-value-annotations | 1.7.4 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.auto.value | auto-value | 1.7.4 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.code.findbugs | jsr305 | 3.0.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.errorprone | error_prone_annotations | 2.0.15 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.guava | failureaccess | 1.0.1 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.guava | guava | 30.1-jre | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.guava | listenablefuture | 9999.0-empty-to-avoid-conflict-with-guava | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.inject | guice | 4.2.3 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.j2objc | j2objc-annotations | 1.3 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.typesafe.netty | netty-reactive-streams-http | 2.0.5 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.typesafe.netty | netty-reactive-streams | 2.0.5 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| commons-codec | commons-codec | 1.15 | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0.txt> |
| commons-logging | commons-logging | 1.1.1 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| io.netty | netty-buffer | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-codec-http2 | 4.1.63.Final | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-codec-http | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-codec | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-common | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-handler | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-resolver | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-transport-native-epoll | 4.1.63.Final | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-transport-native-unix-common | 4.1.63.Final | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-transport | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| javax.inject | javax.inject | 1 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| joda-time | joda-time | 2.6 | Apache 2 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| org.apache.httpcomponents | httpclient | 4.5.13 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| org.apache.httpcomponents | httpcore | 4.4.14 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| org.checkerframework | checker-qual | 3.8.0 | The MIT License | <http://opensource.org/licenses/MIT> |
| org.reactivestreams | reactive-streams | 1.0.3 | CC0 | <http://creativecommons.org/publicdomain/zero/1.0/> |
| org.slf4j | slf4j-api | 1.7.30 | MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j | slf4j-simple | 1.7.30 | MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk | annotations | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | apache-client | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | auth | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | autoscaling | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | aws-core | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | aws-query-protocol | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | connect | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | http-client-spi | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | lambda | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | metrics-spi | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | netty-nio-client | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | pi | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | profiles | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | protocol-core | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | ram | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | regions | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | sdk-core | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | sqs | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | url-connection-client | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | utils | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream | eventstream | 1.0.1 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |

### TerminatedInstanceHandlerLambda_{version}.jar

| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| aopalliance | aopalliance | 1 | Public Domain |
| com.amazonaws | aws-lambda-java-core | 1.2.1 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws | aws-lambda-java-events | 3.8.0 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core | jackson-annotations | 2.12.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.fasterxml.jackson.core | jackson-core | 2.12.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.fasterxml.jackson.core | jackson-databind | 2.12.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.android | annotations | 4.1.1.4 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value | auto-value-annotations | 1.7.4 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.auto.value | auto-value | 1.7.4 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.code.findbugs | jsr305 | 3.0.2 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.errorprone | error_prone_annotations | 2.0.15 | Apache 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.guava | failureaccess | 1.0.1 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.guava | guava | 30.1-jre | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.guava | listenablefuture | 9999.0-empty-to-avoid-conflict-with-guava | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.inject | guice | 4.2.3 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.google.j2objc | j2objc-annotations | 1.3 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.typesafe.netty | netty-reactive-streams-http | 2.0.5 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| com.typesafe.netty | netty-reactive-streams | 2.0.5 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| commons-codec | commons-codec | 1.15 | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0.txt> |
| commons-logging | commons-logging | 1.1.1 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| io.netty | netty-buffer | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-codec-http2 | 4.1.63.Final | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-codec-http | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-codec | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-common | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-handler | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-resolver | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-transport-native-epoll | 4.1.63.Final | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-transport-native-unix-common | 4.1.63.Final | Apache License, Version 2.0 | <https://www.apache.org/licenses/LICENSE-2.0> |
| io.netty | netty-transport | 4.1.53.Final | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| javax.inject | javax.inject | 1 | The Apache Software License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| joda-time | joda-time | 2.6 | Apache 2 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| org.apache.httpcomponents | httpclient | 4.5.13 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| org.apache.httpcomponents | httpcore | 4.4.14 | Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0.txt> |
| org.checkerframework | checker-qual | 3.8.0 | The MIT License | <http://opensource.org/licenses/MIT> |
| org.reactivestreams | reactive-streams | 1.0.3 | CC0 | <http://creativecommons.org/publicdomain/zero/1.0/> |
| org.slf4j | slf4j-api | 1.7.30 | MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j | slf4j-simple | 1.7.30 | MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk | annotations | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | apache-client | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | auth | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | autoscaling | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | aws-core | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | aws-query-protocol | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | connect | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | http-client-spi | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | lambda | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | metrics-spi | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | netty-nio-client | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | pi | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | profiles | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | protocol-core | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | ram | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | regions | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | sdk-core | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | sqs | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | url-connection-client | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk | utils | 2.16.104 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream | eventstream | 1.0.1 | Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |

## License of artifacts in this repository

Apache 2.0 - See [LICENSE](./LICENSE) for more information.
