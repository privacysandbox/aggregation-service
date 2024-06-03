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

load("@rules_java//java:defs.bzl", "java_library", "java_plugin")

package(
    default_visibility = ["//visibility:public"],
)

alias(
    name = "guava",
    actual = "@maven//:com_google_guava_guava",
)

alias(
    name = "guava_testlib",
    actual = "@maven//:com_google_guava_guava_testlib",
)

alias(
    name = "guice",
    actual = "@maven//:com_google_inject_guice",
)

alias(
    name = "autoservice_processor",
    actual = "@maven//:com_google_auto_service_auto_service",
)

alias(
    name = "autoservice_annotations",
    actual = "@maven//:com_google_auto_service_auto_service_annotations",
)

java_plugin(
    name = "autoservice_plugin",
    processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
    deps = [":autoservice_processor"],
)

java_library(
    name = "autoservice",
    exported_plugins = [
        ":autoservice_plugin",
    ],
    javacopts = ["-Xep:Var"],
    neverlink = True,
    exports = [":autoservice_annotations"],
)

alias(
    name = "findbugs",
    actual = "@maven//:com_google_code_findbugs_jsr305",
)

alias(
    name = "hamcrest",
    actual = "@maven//:org_hamcrest_hamcrest_library",
)

alias(
    name = "jackson_annotations",
    actual = "@maven//:com_fasterxml_jackson_core_jackson_annotations",
)

alias(
    name = "jackson_databind",
    actual = "@maven//:com_fasterxml_jackson_core_jackson_databind",
)

alias(
    name = "jackson_core",
    actual = "@maven//:com_fasterxml_jackson_core_jackson_core",
)

alias(
    name = "jackson_datatype_guava",
    actual = "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_guava",
)

alias(
    name = "jackson_datatype_jdk8",
    actual = "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jdk8",
)

alias(
    name = "jackson_datatype_jsr310",
    actual = "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_jsr310",
)

alias(
    name = "jackson_dataformat_cbor",
    actual = "@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_cbor",
)

alias(
    name = "javax_annotation",
    actual = "@maven//:javax_annotation_javax_annotation_api",
)

alias(
    name = "javax_inject",
    actual = "@maven//:javax_inject_javax_inject",
)

alias(
    name = "jayway_jsonpath",
    actual = "@maven//:com_jayway_jsonpath_json_path",
)

alias(
    name = "jimfs",
    actual = "@maven//:com_google_jimfs_jimfs",
)

alias(
    name = "junit",
    actual = "@maven//:junit_junit",
)

alias(
    name = "google_truth",
    actual = "@maven//:com_google_truth_truth",
)

alias(
    name = "google_auth_library_oauth2_http",
    actual = "@maven//:com_google_auth_google_auth_library_oauth2_http",
)

alias(
    name = "test_parameter_injector",
    actual = "@maven//:com_google_testparameterinjector_test_parameter_injector",
)

alias(
    name = "google_truth_proto",
    actual = "@maven//:com_google_truth_extensions_truth_proto_extension",
)

alias(
    name = "google_truth8",
    actual = "@maven//:com_google_truth_extensions_truth_java8_extension",
)

alias(
    name = "autovalue_processor",
    actual = "@maven//:com_google_auto_value_auto_value",
)

alias(
    name = "autovalue_annotations",
    actual = "@maven//:com_google_auto_value_auto_value_annotations",
)

java_plugin(
    name = "autovalue_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = [":autovalue_processor"],
)

java_plugin(
    name = "autooneof_plugin",
    processor_class = "com.google.auto.value.processor.AutoOneOfProcessor",
    deps = [":autovalue_processor"],
)

java_library(
    name = "autovalue",
    exported_plugins = [
        ":autooneof_plugin",
        ":autovalue_plugin",
    ],
    javacopts = ["-Xep:Var"],
    neverlink = True,
    exports = [":autovalue_annotations"],
)

alias(
    name = "autofactory_library",
    actual = "@maven//:com_google_auto_factory_auto_factory",
)

java_plugin(
    name = "autofactory_plugin",
    generates_api = True,
    processor_class = "com.google.auto.factory.processor.AutoFactoryProcessor",
    deps = [
        ":autofactory_library",
    ],
)

java_library(
    name = "autofactory",
    exported_plugins = [":autofactory_plugin"],
    javacopts = ["-Xep:Var"],
    neverlink = True,
    exports = [":autofactory_library"],
)

alias(
    name = "acai",
    actual = "@maven//:com_google_acai_acai",
)

alias(
    name = "jcommander",
    actual = "@maven//:com_beust_jcommander",
)

alias(
    name = "avro",
    actual = "@maven//:org_apache_avro_avro",
)

alias(
    name = "apache_httpclient",
    actual = "@maven//:org_apache_httpcomponents_httpclient",
)

alias(
    name = "apache_httpclient5",
    actual = "@maven//:org_apache_httpcomponents_client5_httpclient5",
)

alias(
    name = "apache_httpcore",
    actual = "@maven//:org_apache_httpcomponents_httpcore",
)

alias(
    name = "apache_httpcore5",
    actual = "@maven//:org_apache_httpcomponents_core5_httpcore5",
)

alias(
    name = "aws_sdk",
    actual = "@maven//:software_amazon_awssdk_aws_sdk_java",
)

alias(
    name = "aws_ec2",
    actual = "@maven//:software_amazon_awssdk_ec2",
)

alias(
    name = "aws_s3",
    actual = "@maven//:software_amazon_awssdk_s3",
)

alias(
    name = "aws_netty_nio_client",
    actual = "@maven//:software_amazon_awssdk_netty_nio_client",
)

alias(
    name = "aws_s3_transfer_manager",
    actual = "@maven//:software_amazon_awssdk_s3_transfer_manager",
)

alias(
    name = "aws_ssm",
    actual = "@maven//:software_amazon_awssdk_ssm",
)

alias(
    name = "aws_sts",
    actual = "@maven//:software_amazon_awssdk_sts",
)

alias(
    name = "aws_sqs",
    actual = "@maven//:software_amazon_awssdk_sqs",
)

alias(
    name = "aws_regions",
    actual = "@maven//:software_amazon_awssdk_regions",
)

alias(
    name = "aws_dynamodb",
    actual = "@maven//:software_amazon_awssdk_dynamodb",
)

alias(
    name = "aws_dynamodb_enhanced",
    actual = "@maven//:software_amazon_awssdk_dynamodb_enhanced",
)

alias(
    name = "aws_lambda_java_core",
    actual = "@maven//:com_amazonaws_aws_lambda_java_core",
)

alias(
    name = "aws_lambda_java_events",
    actual = "@maven//:com_amazonaws_aws_lambda_java_events",
)

alias(
    name = "aws_lambda_java_events_sdk_transformer",
    actual = "@maven//:com_amazonaws_aws_lambda_java_events_sdk_transformer",
)

alias(
    name = "slf4j",
    actual = "@maven//:org_slf4j_slf4j_api",
)

alias(
    name = "slf4j_simple",
    actual = "@maven//:org_slf4j_slf4j_simple",
)

alias(
    name = "slf4j_log4j",
    actual = "@maven//:org_slf4j_slf4j_log4j12",
    # log4j is only used in tests.
    visibility = ["//javatests:__subpackages__"],
)

alias(
    name = "log4j_12_api",
    actual = "@maven//:org_apache_logging_log4j_log4j_1_2_api",
    # log4j is only used in tests.
    visibility = ["//javatests:__subpackages__"],
)

alias(
    name = "log4j_core",
    actual = "@maven//:org_apache_logging_log4j_log4j_core",
    # log4j is only used in tests.
    visibility = ["//javatests:__subpackages__"],
)

alias(
    name = "differential_privacy",
    actual = "@com_google_differential_privacy//java/main/com/google/privacy/differentialprivacy",
)

alias(
    name = "differential_privacy_reference_noise",
    actual = "@com_google_differential_privacy//java/main/com/google/privacy/differentialprivacy/testing:reference_noise",
)

alias(
    name = "differential_privacy_statistical_utils",
    actual = "@com_google_differential_privacy//java/main/com/google/privacy/differentialprivacy/testing:statistical_utils",
)

alias(
    name = "clients_blobstorageclient",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient",
)

alias(
    name = "clients_blobstorageclient_model",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient:model",
)

alias(
    name = "clients_blobstorageclient_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient:aws",
)

alias(
    name = "jobqueue_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/jobqueue/gcp:gcp",
)

alias(
    name = "shared_dao_gcp",
    actual = "@shared_libraries//java/com/google/scp/coordinator/keymanagement/shared/dao/gcp:gcp",
)

alias(
    name = "clients_jobclient",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/jobclient",
)

alias(
    name = "clients_jobclient_model",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/jobclient:model",
)

alias(
    name = "clients_jobclient_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/jobclient:aws",
)

alias(
    name = "clients_lifecycleclient_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/lifecycleclient:aws",
)

alias(
    name = "clients_lifecycleclient_local",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/lifecycleclient:local",
)

alias(
    name = "clients_metricclient",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/metricclient",
)

alias(
    name = "clients_metricclient_model",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/metricclient:model",
)

alias(
    name = "clients_metricclient_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/metricclient:aws",
)

alias(
    name = "clients_distributed_privacy_budgeting",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/distributedprivacybudgetclient:distributed_privacy_budget_client",
)

alias(
    name = "clients_distributed_privacy_budgeting_local",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/distributedprivacybudgetclient/local:local_distributed_privacy_budget_client",
)

alias(
    name = "clients_distributed_privacy_budgeting_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/distributedprivacybudgetclient/aws:aws_pbs_client_module",
)

alias(
    name = "clients_distributed_privacy_budgeting_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/distributedprivacybudgetclient/gcp:gcp_pbs_client_module",
)

alias(
    name = "model_privacy_budgeting",
    actual = "@shared_libraries//java/com/google/scp/coordinator/privacy/budgeting/model",
)

alias(
    name = "dao_metadatadb_aws_model",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/metadatadb/aws/model",
)

alias(
    name = "dao_metadatadb_aws_model_converter",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/metadatadb/aws/model/converter",
)

alias(
    name = "dao_metadatadb_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/metadatadb/aws",
)

alias(
    name = "dao_metadatadb_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/metadatadb/gcp",
)

alias(
    name = "dao_metadatadb_common",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/metadatadb/common",
)

alias(
    name = "dao_metadatadb_testing_jobgenerator",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/metadatadb/testing:job_generator",
)

alias(
    name = "dao_metadatadb_testing_fakemetadatadb",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/metadatadb/testing:fake_metadata_db",
)

alias(
    name = "dao_jobqueue_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/jobqueue/aws",
)

alias(
    name = "dao_jobqueue_common",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/jobqueue/common",
)

alias(
    name = "dao_jobqueue_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/jobqueue/gcp",
)

alias(
    name = "dao_jobqueue_testing",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/dao/jobqueue/testing",
)

alias(
    name = "frontend_testing",
    actual = "@shared_libraries//java/com/google/scp/operator/frontend/testing",
)

alias(
    name = "frontend_testing_fake_jobmetadata_change_handler",
    actual = "@shared_libraries//java/com/google/scp/operator/frontend/testing:fake_jobmetadata_change_handler",
)

alias(
    name = "frontend_validation",
    actual = "@shared_libraries//java/com/google/scp/operator/frontend/tasks/validation",
)

alias(
    name = "frontend_java_proto",
    actual = "@shared_libraries//operator/protos/frontend/api/v1:java_proto",
)

alias(
    name = "shared_model",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/model",
)

alias(
    name = "clients_configclient",
    actual = "@shared_libraries//java/com/google/scp/shared/clients/configclient",
)

alias(
    name = "clients_configclient_operator",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/configclient",
)

alias(
    name = "shared_testing",
    actual = "@shared_libraries//java/com/google/scp/operator/shared/testing",
)

alias(
    name = "clients_configclient_aws",
    actual = "@shared_libraries//java/com/google/scp/shared/clients/configclient:aws",
)

alias(
    name = "clients_configclient_aws_operator",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/configclient:aws",
)

alias(
    name = "clients_configclient_model",
    actual = "@shared_libraries//java/com/google/scp/shared/clients/configclient:model",
)

alias(
    name = "clients_cryptoclient",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/cryptoclient",
)

alias(
    name = "clients_cryptoclient_aws",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/cryptoclient:aws",
)

alias(
    name = "clients_blobstorageclient_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/blobstorageclient:gcp",
)

alias(
    name = "clients_jobclient_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/jobclient:gcp",
)

alias(
    name = "clients_metricclient_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/metricclient:gcp",
)

alias(
    name = "clients_configclient_gcp",
    actual = "@shared_libraries//java/com/google/scp/shared/clients/configclient:gcp",
)

alias(
    name = "clients_configclient_gcp_operator",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/configclient:gcp",
)

alias(
    name = "clients_cryptoclient_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/cryptoclient:gcp",
)

alias(
    name = "worker_decryption",
    actual = "@shared_libraries//java/com/google/scp/operator/worker/decryption",
)

alias(
    name = "worker_hybrid_decryption",
    actual = "@shared_libraries//java/com/google/scp/operator/worker/decryption/hybrid",
)

alias(
    name = "operator_worker_reader",
    actual = "@shared_libraries//java/com/google/scp/operator/worker/reader",
)

alias(
    name = "operator_worker_reader_avro",
    actual = "@shared_libraries//java/com/google/scp/operator/worker/reader/avro",
)

alias(
    name = "operator_report_serdes",
    actual = "@shared_libraries//java/com/google/scp/operator/worker/model/serdes",
)

alias(
    name = "operator_report_serdes_proto",
    actual = "@shared_libraries//java/com/google/scp/operator/worker/model/serdes/proto:report_serdes",
)

alias(
    name = "shared_mapper",
    actual = "@shared_libraries//java/com/google/scp/shared/mapper",
)

alias(
    name = "clients_lifecycleclient_gcp",
    actual = "@shared_libraries//java/com/google/scp/operator/cpio/lifecycleclient:gcp",
)

alias(
    name = "crypto_tink_integration_awskmsv2",
    actual = "@shared_libraries//java/com/google/crypto/tink/integration/awskmsv2",
)

alias(
    name = "keymanagement_keyhosting_java_proto",
    actual = "@shared_libraries//coordinator/protos/keymanagement/keyhosting/api/v1:java_proto",
)

alias(
    name = "keymanagement_shared_util",
    actual = "@shared_libraries//java/com/google/scp/shared/util",
)

alias(
    name = "api_shared_util",
    actual = "@shared_libraries//java/com/google/scp/shared/api/util:util",
)

alias(
    name = "tink",
    # Sync Tink version with SCP repo's
    # TODO: This is no longer needed once the changes SCP depends on are
    # published to Maven.
    actual = "@shared_libraries//java/external:tink",
)

alias(
    name = "gcp-http-client-jackson2",
    actual = "@maven//:com_google_http_client_google_http_client_jackson2",
)

alias(
    name = "aws_core",
    actual = "@maven//:software_amazon_awssdk_sdk_core",
)

alias(
    name = "aws_auth",
    actual = "@maven//:software_amazon_awssdk_auth",
)

alias(
    name = "aws_http_client_spi",
    actual = "@maven//:software_amazon_awssdk_http_client_spi",
)

alias(
    name = "aws_kms",
    actual = "@maven//:software_amazon_awssdk_kms",
)

alias(
    name = "aws_profiles",
    actual = "@maven//:software_amazon_awssdk_profiles",
)

alias(
    name = "aws_utils",
    actual = "@maven//:software_amazon_awssdk_utils",
)

alias(
    name = "aws_url_connection_client",
    actual = "@maven//:software_amazon_awssdk_url_connection_client",
)

alias(
    name = "aws_apache_http",
    actual = "@maven//:software_amazon_awssdk_apache_client",
)

alias(
    name = "aws_lambda_v2",
    actual = "@maven//:software_amazon_awssdk_lambda",
)

alias(
    name = "mockserver_core",
    actual = "@maven//:org_mock_server_mockserver_core",
)

alias(
    name = "mockserver_client",
    actual = "@maven//:org_mock_server_mockserver_client_java",
)

alias(
    name = "mockserver_junit_rule",
    actual = "@maven//:org_mock_server_mockserver_junit_rule",
)

alias(
    name = "testcontainers",
    actual = "@maven//:org_testcontainers_testcontainers",
)

alias(
    name = "gcp_core",
    actual = "@maven//:com_google_cloud_google_cloud_core",
)

alias(
    name = "gcp_storage",
    actual = "@maven//:com_google_cloud_google_cloud_storage",
)

alias(
    name = "google_gax",
    actual = "@maven//:com_google_api_gax",
)

alias(
    name = "localstack_containers",
    actual = "@maven//:org_testcontainers_localstack",
)

alias(
    # localstack_containers uses this dependency in the background.
    # We do not directly use this outdated version of the API.
    name = "aws_sqs_java_sdk1",
    actual = "@maven//:com_amazonaws_aws_java_sdk_sqs",
    visibility = ["//javatests:__subpackages__"],
)

alias(
    # localstack_containers uses this dependency in the background.
    # We do not directly use this outdated version of the API.
    name = "aws_s3_java_sdk1",
    actual = "@maven//:com_amazonaws_aws_java_sdk_s3",
    visibility = ["//javatests:__subpackages__"],
)

alias(
    # localstack_containers uses this dependency in the background.
    # We do not directly use this outdated version of the API.
    name = "aws_kms_java_sdk1",
    actual = "@maven//:com_amazonaws_aws_java_sdk_kms",
    visibility = ["//javatests:__subpackages__"],
)

alias(
    # localstack_containers uses this dependency in the background.
    # We do not directly use this outdated version of the API.
    name = "aws_core_java_sdk1",
    actual = "@maven//:com_amazonaws_aws_java_sdk_core",
    visibility = ["//javatests:__subpackages__"],
)

alias(
    name = "mockito",
    actual = "@maven//:org_mockito_mockito_core",
    visibility = [
        "//javatests:__subpackages__",
        "//telemetry/debug/javatests:__subpackages__",
        "//telemetry/prod/javatests:__subpackages__",
    ],
)

alias(
    name = "gcp_spanner",
    actual = "@maven//:com_google_cloud_google_cloud_spanner",
)

alias(
    name = "gcp_compute",
    actual = "@maven//:com_google_cloud_google_cloud_compute",
)

alias(
    name = "gcp_kms",
    actual = "@maven//:com_google_cloud_google_cloud_kms",
)

alias(
    name = "gcp_pubsub",
    actual = "@maven//:com_google_cloud_google_cloud_pubsub",
)

alias(
    name = "proto_gcp_compute_v1",
    actual = "@maven//:com_google_api_grpc_proto_google_cloud_compute_v1",
)

alias(
    name = "proto_gcp_common",
    actual = "@maven//:com_google_api_grpc_proto_google_common_protos",
)

alias(
    name = "gcp_pubsub_v1",
    actual = "@maven//:com_google_api_grpc_proto_google_cloud_pubsub_v1",
)

alias(
    name = "gcp_api_common",
    actual = "@maven//:com_google_api_api_common",
)

alias(
    name = "gcp_api_gax",
    actual = "@maven//:com_google_api_gax",
)

alias(
    name = "grpc_api",
    actual = "@maven//:io_grpc_grpc_api",
)

alias(
    name = "gcp_gax_grpc",
    actual = "@maven//:com_google_api_gax_grpc",
)

alias(
    name = "gcp_monitoring",
    actual = "@maven//:com_google_cloud_google_cloud_monitoring",
)

alias(
    name = "gcp_proto_cloud_monitoring",
    actual = "@maven//:com_google_api_grpc_proto_google_cloud_monitoring_v3",
)

alias(
    name = "protobuf_java",
    actual = "@maven//:com_google_protobuf_protobuf_java",
)

alias(
    name = "protobuf_java_util",
    actual = "@maven//:com_google_protobuf_protobuf_java_util",
)

alias(
    name = "commons_logging",
    actual = "@maven//:commons_logging_commons_logging",
)

alias(
    name = "awaitility",
    actual = "@maven//:org_awaitility_awaitility",
    visibility = ["//javatests:__subpackages__"],
)

alias(
    name = "aws_core2",
    actual = "@maven//:software_amazon_awssdk_aws_core",
)

alias(
    name = "aws_xray",
    actual = "@maven//:com_amazonaws_aws_java_sdk_xray",
)

alias(
    name = "aws_cloudwatch",
    actual = "@maven//:com_amazonaws_aws_java_sdk_cloudwatch",
)

alias(
    name = "aws_auto_scaling",
    actual = "@maven//:software_amazon_awssdk_autoscaling",
)

alias(
    name = "scp_shared_proto",
    actual = "@shared_libraries//java/com/google/scp/shared/proto",
)

alias(
    name = "keymanagement_shared_proto",
    actual = "@shared_libraries//coordinator/protos/keymanagement/shared/backend:proto",
)

alias(
    name = "operator_protos",
    actual = "@shared_libraries//operator/protos/shared/backend:java_proto",
)

alias(
    name = "operator_metadatadb_protos",
    actual = "@shared_libraries//operator/protos/shared/backend/metadatadb:java_proto",
)

alias(
    name = "rxjava",
    actual = "@maven//:io_reactivex_rxjava3_rxjava",
)

alias(
    name = "error_prone_anntotations",
    actual = "@maven//:com_google_errorprone_error_prone_annotations",
)

alias(
    name = "opentelemetry_api",
    actual = "@maven//:io_opentelemetry_opentelemetry_api",
)

alias(
    name = "opentelemetry_context",
    actual = "@maven//:io_opentelemetry_opentelemetry_context",
)

alias(
    name = "opentelemetry_exporter_logging",
    actual = "@maven//:io_opentelemetry_opentelemetry_exporter_logging",
)

alias(
    name = "opentelemetry_exporter_logging_otlp",
    actual = "@maven//:io_opentelemetry_opentelemetry_exporter_logging_otlp",
)

alias(
    name = "opentelemetry_exporter_otlp",
    actual = "@maven//:io_opentelemetry_opentelemetry_exporter_otlp",
)

alias(
    name = "opentelemetry_sdk",
    actual = "@maven//:io_opentelemetry_opentelemetry_sdk",
)

alias(
    name = "opentelemetry_sdk_common",
    actual = "@maven//:io_opentelemetry_opentelemetry_sdk_common",
)

alias(
    name = "opentelemetry_sdk_metrics",
    actual = "@maven//:io_opentelemetry_opentelemetry_sdk_metrics",
)

alias(
    name = "opentelemetry_sdk_testing",
    actual = "@maven//:io_opentelemetry_opentelemetry_sdk_testing",
)

alias(
    name = "opentelemetry_sdk_trace",
    actual = "@maven//:io_opentelemetry_opentelemetry_sdk_trace",
)

alias(
    name = "opentelemetry_contrib_aws_xray",
    actual = "@maven//:io_opentelemetry_contrib_opentelemetry_aws_xray",
)

alias(
    name = "scp_credsprovider",
    actual = "@shared_libraries//java/com/google/scp/shared/aws/credsprovider:credsprovider",
)

alias(
    name = "gcp_trace",
    actual = "@maven//:com_google_cloud_google_cloud_trace",
)

alias(
    name = "gcp_trace_proto",
    actual = "@maven//:com_google_api_grpc_proto_google_cloud_trace_v1",
)
