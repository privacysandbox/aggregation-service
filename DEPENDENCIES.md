# Dependencies and Licenses

## AWS

The deployment of the Amazon Web Services
[Nitro Enclaves](https://aws.amazon.com/ec2/nitro/nitro-enclaves/) based Aggregation Service depends
on several packaged artifacts listed below. These artifacts can be downloaded with the
[download_prebuilt_dependencies.sh](/terraform/aws/download_prebuilt_dependencies.sh) script. More
information can be found in the
[README](/docs/aws-aggregation-service.md#download-terraform-scripts-and-prebuilt-dependencies).

### Packaged AWS Lambda Jars

#### AsgCapacityHandlerLambda\_{version}.jar

<!-- prettier-ignore-start -->
| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| com.amazonaws|aws-lambda-java-core|1.2.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events-sdk-transformer|3.1.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-core|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-databind|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value|auto-value-annotations|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.findbugs|jsr305|3.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.gson|gson|2.10.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.errorprone|error-prone-annotations|2.24.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|failureaccess|1.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|guava|33.0.0-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.j2objc|j2objc-annotations|2.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| commons-codec|commons-codec|1.16|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0> |
| commons-logging|commons-logging|1.3.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-buffer|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-common|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-classes-epoll|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.100.Final|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| joda-time|joda-time|2.10.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpclient|4.5.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpcore|4.4.16|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.jctools|jctools-core|3.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.slf4j|slf4j-api|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j|slf4j-simple|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|endpoints-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-aws|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|identity-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|json-utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
<!-- prettier-ignore-end -->

#### AwsApigatewayFrontend\_{version}.jar

<!-- prettier-ignore-start -->
| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| com.amazonaws|aws-lambda-java-core|1.2.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events|3.11.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events-sdk-transformer|3.1.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-core|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-databind|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.service|auto-service-annotations|1.1.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value|auto-value-annotations|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.findbugs|jsr305|3.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.gson|gson|2.10.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.errorprone|error-prone-annotations|2.24.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|failureaccess|1.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|guava|33.0.0-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.j2objc|j2objc-annotations|2.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| commons-codec|commons-codec|1.16|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0> |
| commons-logging|commons-logging|1.3.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-buffer|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-common|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-classes-epoll|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.100.Final|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr-match|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| joda-time|joda-time|2.10.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpclient|4.5.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpcore|4.4.16|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.client5|httpclient5|5.3|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5-h2|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.jctools|jctools-core|3.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.slf4j|slf4j-api|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j|slf4j-simple|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|arns|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-xml-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|crt-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|endpoints-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-aws|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|identity-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|json-utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|kms|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|pricing|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|s3|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sts|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
<!-- prettier-ignore-end -->

#### AwsChangeHandlerLambda\_{version}.jar

<!-- prettier-ignore-start -->
| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| com.amazonaws|aws-lambda-java-core|1.2.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events-sdk-transformer|3.1.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-core|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-databind|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.service|auto-service-annotations|1.1.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value|auto-value-annotations|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value|auto-value|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.findbugs|jsr305|3.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.gson|gson|2.10.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.errorprone|error-prone-annotations|2.24.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|failureaccess|1.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|guava|33.0.0-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.j2objc|j2objc-annotations|2.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| commons-codec|commons-codec|1.16|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0> |
| commons-logging|commons-logging|1.3.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-buffer|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-common|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-classes-epoll|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.100.Final|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr-match|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| joda-time|joda-time|2.10.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpclient|4.5.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpcore|4.4.16|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.client5|httpclient5|5.3|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5-h2|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.jctools|jctools-core|3.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.slf4j|slf4j-api|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j|slf4j-simple|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|arns|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-xml-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|crt-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|endpoints-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-aws|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|identity-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|json-utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|kms|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|pricing|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|s3|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sts|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
<!-- prettier-ignore-end -->

#### AwsFrontendCleanupLambda\_{version}.jar

<!-- prettier-ignore-start -->
| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| com.amazonaws|aws-lambda-java-core|1.2.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events|3.11.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events-sdk-transformer|3.1.0|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-core|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-databind|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.service|auto-service-annotations|1.1.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value|auto-value-annotations|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.findbugs|jsr305|3.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.gson|gson|2.10.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.errorprone|error-prone-annotations|2.24.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|failureaccess|1.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|guava|33.0.0-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.j2objc|j2objc-annotations|2.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| commons-codec|commons-codec|1.16|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0> |
| commons-logging|commons-logging|1.3.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-buffer|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-common|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-classes-epoll|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.100.Final|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr-match|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| joda-time|joda-time|2.10.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpclient|4.5.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpcore|4.4.16|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.client5|httpclient5|5.3|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5-h2|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.jctools|jctools-core|3.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.slf4j|slf4j-api|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j|slf4j-simple|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|arns|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-xml-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|crt-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|endpoints-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-aws|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|identity-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|json-utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|kms|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|pricing|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|s3|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sts|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
<!-- prettier-ignore-end -->

#### TerminatedInstanceHandlerLambda\_{version}.jar

<!-- prettier-ignore-start -->
| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| com.amazonaws|aws-lambda-java-core|1.2.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.amazonaws|aws-lambda-java-events|3.11.3|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| com.fasterxml.jackson.core|jackson-annotations|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-core|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-databind|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auto.value|auto-value-annotations|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.findbugs|jsr305|3.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.gson|gson|2.10.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.errorprone|error-prone-annotations|2.24.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|failureaccess|1.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|guava|33.0.0-jre|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.j2objc|j2objc-annotations|2.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| commons-codec|commons-codec|1.16|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0> |
| commons-logging|commons-logging|1.3.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-buffer|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-common|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-classes-epoll|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.100.Final|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| joda-time|joda-time|2.10.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpclient|4.5.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpcore|4.4.16|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.jctools|jctools-core|3.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.slf4j|slf4j-api|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j|slf4j-simple|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| software.amazon.awssdk|annotations|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|apache-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|autoscaling|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-json-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|aws-query-protocol|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|checksums-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|dynamodb-enhanced|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|endpoints-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-aws|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-auth-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|http-client-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|identity-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|json-utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|metrics-spi|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|netty-nio-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|profiles|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|protocol-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|regions|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sdk-core|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|sqs|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|url-connection-client|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.awssdk|utils|2.21.16|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
| software.amazon.eventstream|eventstream|1.0.1|Apache License, Version 2.0 | <https://aws.amazon.com/apache2.0> |
<!-- prettier-ignore-end -->

## GCP

The deployment of the Google Cloud Platform
[Confidential Space](https://cloud.google.com/blog/products/identity-security/confidential-space-is-ga)
based Aggregation Service depends on several packaged artifacts listed below. These artifacts can be
downloaded with the
[download_prebuilt_dependencies.sh](/terraform/gcp/download_prebuilt_dependencies.sh) script. More
information can be found in the
[README](/docs/gcp-aggregation-service.md#download-terraform-scripts-and-prebuilt-dependencies).

### Packaged GCP Cloud Function Jars

#### FrontendServiceHttpCloudFunction\_{version}.jar

<!-- prettier-ignore-start -->
| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| com.fasterxml.jackson.core|jackson-annotations|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-core|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-databind|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.android|annotations|4.1.1.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api|gax|2.41.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.api|gax-grpc|2.41.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.api|gax-httpjson|2.41.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.api-client|google-api-client|2.2.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|grpc-google-cloud-spanner-admin-database-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|grpc-google-cloud-spanner-admin-instance-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|grpc-google-cloud-spanner-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|grpc-google-common-protos|2.31.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-pubsub-v1|1.108.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-admin-database-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-admin-instance-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-executor-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-common-protos|2.32.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-iam-v1|1.27.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auth|google-auth-library-credentials|1.22.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.auth|google-auth-library-oauth2-http|1.22.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.auto.value|auto-value-annotations|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-core|2.31.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-core-grpc|2.31.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-pubsub|1.126.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-spanner|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud.functions|functions-framework-api|1.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.findbugs|jsr305|3.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.gson|gson|2.10.1|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.errorprone|error-prone-annotations|2.24.1|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|failureaccess|1.0.2|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|guava|33.0.0-jre|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.http-client|google-http-client|1.43.3|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.http-client|google-http-client-gson|1.43.3|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.j2objc|j2objc-annotations|2.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.oauth-client|google-oauth-client|1.35.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| commons-codec|commons-codec|1.16|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0> |
| commons-logging|commons-logging|1.3.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.cloudevents|cloudevents-api|2.5.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-buffer|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-common|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-classes-epoll|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.100.Final|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr-match|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| javax.annotation|javax.annotation-api|1.3.2|CDDL, GPL 2.0 |<https://github.com/javaee/javax.annotation/blob/1.3.2/LICENSE> |
| org.apache.httpcomponents|httpclient|4.5.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpcore|4.4.16|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.client5|httpclient5|5.3|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5-h2|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.codehaus.mojo|animal-sniffer-annotations|1.23|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.jctools|jctools-core|3.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.slf4j|slf4j-api|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j|slf4j-simple|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.threeten|threetenbp|1.6.8|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
<!-- prettier-ignore-end -->

#### WorkerScaleInCloudFunction\_{version}.jar

<!-- prettier-ignore-start -->
| groupId | artifactId | Version | License | URL |
|--|--|--|--|--|
| com.fasterxml.jackson.core|jackson-annotations|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-core|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.core|jackson-databind|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-guava|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.16.1|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.android|annotations|4.1.1.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api|gax|2.41.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.api|gax-grpc|2.41.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.api|gax-httpjson|2.41.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.api.grpc|grpc-google-cloud-spanner-admin-database-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|grpc-google-cloud-spanner-admin-instance-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|grpc-google-cloud-spanner-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|grpc-google-common-protos|2.31.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloudcompute-v1|1.44.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-pubsub-v1|1.108.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-admin-database-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-admin-instance-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-executor-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-cloud-spanner-v1|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-common-protos|2.32.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.api.grpc|proto-google-iam-v1|1.27.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.auth|google-auth-library-credentials|1.22.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.auth|google-auth-library-oauth2-http|1.22.0|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
| com.google.auto.value|auto-value-annotations|1.10.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-compute|1.44.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-core|2.31.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-core-grpc|2.31.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud|google-cloud-spanner|6.56.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.cloud.functions|functions-framework-api|1.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.findbugs|jsr305|3.0.2|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.code.gson|gson|2.10.1|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.errorprone|error-prone-annotations|2.24.1|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|failureaccess|1.0.2|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|guava|33.0.0-jre|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.http-client|google-http-client|1.43.3|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.http-client|google-http-client-gson|1.43.3|Apache License, Version 2.0  | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.j2objc|j2objc-annotations|2.8|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| com.google.oauth-client|google-oauth-client|1.35.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| commons-codec|commons-codec|1.16|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0> |
| commons-logging|commons-logging|1.3.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.cloudevents|cloudevents-api|2.5.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-buffer|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-codec-http2|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-common|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-handler|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-resolver|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport|4.1.100.Final|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| io.netty|netty-transport-native-classes-epoll|4.1.100.Final|Apache License, Version 2.0|<https://www.apache.org/licenses/LICENSE-2.0>
| io.netty|netty-transport-native-unix-common|4.1.100.Final|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| io.vavr|vavr-match|0.10.2|Apache License, Version 2.0 |<https://www.apache.org/licenses/LICENSE-2.0>
| javax.annotation|javax.annotation-api|1.3.2|CDDL, GPL 2.0 |<https://github.com/javaee/javax.annotation/blob/1.3.2/LICENSE> |
| org.apache.httpcomponents|httpclient|4.5.14|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents|httpcore|4.4.16|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.client5|httpclient5|5.3|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.apache.httpcomponents.core5|httpcore5-h2|5.2.4|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.codehaus.mojo|animal-sniffer-annotations|1.23|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.jctools|jctools-core|3.1.0|Apache License, Version 2.0 | <http://www.apache.org/licenses/LICENSE-2.0> |
| org.slf4j|slf4j-api|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.slf4j|slf4j-simple|2.0.11|MIT License | <http://www.opensource.org/licenses/mit-license.php> |
| org.threeten|threetenbp|1.6.8|BSD 3-Clause | <https://licenses.nuget.org/BSD-3-Clause> |
<!-- prettier-ignore-end -->

## License of artifacts in this repository

Apache License, Version 2.0 - See [LICENSE](./LICENSE) for more information.
