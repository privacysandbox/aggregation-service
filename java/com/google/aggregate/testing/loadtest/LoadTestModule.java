/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.aggregate.testing.loadtest;

import com.google.aggregate.privacy.budgeting.bridge.HttpPrivacyBudgetingServiceBridge;
import com.google.aggregate.privacy.budgeting.bridge.PrivacyBudgetingServiceBridge;
import com.google.inject.AbstractModule;
import com.google.scp.operator.cpio.configclient.Annotations.CoordinatorARegionBinding;
import com.google.scp.operator.cpio.configclient.Annotations.CoordinatorBRegionBinding;
import com.google.scp.operator.cpio.configclient.aws.Annotations.CoordinatorACredentialsProvider;
import com.google.scp.operator.cpio.configclient.aws.Annotations.CoordinatorBCredentialsProvider;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorAPrivacyBudgetServiceAuthEndpoint;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorAPrivacyBudgetServiceBaseUrl;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorBPrivacyBudgetServiceAuthEndpoint;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.DistributedPrivacyBudgetClientModule.CoordinatorBPrivacyBudgetServiceBaseUrl;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.TransactionPhaseManager;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.TransactionPhaseManagerImpl;
import com.google.scp.operator.cpio.distributedprivacybudgetclient.aws.AwsPbsClientModule;
import com.google.scp.shared.aws.credsprovider.AwsSessionCredentialsProvider;
import com.google.scp.shared.aws.credsprovider.StsAwsSessionCredentialsProvider;
import com.google.scp.shared.clients.configclient.aws.AwsClientConfigModule.AwsCredentialAccessKey;
import com.google.scp.shared.clients.configclient.aws.AwsClientConfigModule.AwsCredentialSecretKey;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

public final class LoadTestModule extends AbstractModule {

  private final LoadTestArgs cliArgs;

  public LoadTestModule(LoadTestArgs cliArgs) {
    this.cliArgs = cliArgs;
  }

  @Override
  protected void configure() {
    bind(PrivacyBudgetingServiceBridge.class).to(HttpPrivacyBudgetingServiceBridge.class);
    bind(String.class)
        .annotatedWith(CoordinatorAPrivacyBudgetServiceBaseUrl.class)
        .toInstance(cliArgs.getCoordinatorAPrivacyBudgetingEndpoint());
    bind(String.class)
        .annotatedWith(CoordinatorBPrivacyBudgetServiceBaseUrl.class)
        .toInstance(cliArgs.getCoordinatorBPrivacyBudgetingEndpoint());
    bind(String.class)
        .annotatedWith(CoordinatorAPrivacyBudgetServiceAuthEndpoint.class)
        .toInstance(cliArgs.getCoordinatorAPrivacyBudgetServiceAuthEndpoint());
    bind(String.class)
        .annotatedWith(CoordinatorBPrivacyBudgetServiceAuthEndpoint.class)
        .toInstance(cliArgs.getCoordinatorBPrivacyBudgetServiceAuthEndpoint());
    StsClient stsClient =
        StsClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build();
    StsAwsSessionCredentialsProvider credsProviderA =
        new StsAwsSessionCredentialsProvider(
            stsClient, cliArgs.getCoordinatorARoleArn(), "coordinatorA");
    StsAwsSessionCredentialsProvider credsProviderB =
        new StsAwsSessionCredentialsProvider(
            stsClient, cliArgs.getCoordinatorBRoleArn(), "coordinatorB");
    bind(AwsSessionCredentialsProvider.class)
        .annotatedWith(CoordinatorACredentialsProvider.class)
        .toInstance(credsProviderA);
    bind(AwsSessionCredentialsProvider.class)
        .annotatedWith(CoordinatorBCredentialsProvider.class)
        .toInstance(credsProviderB);
    bind(String.class).annotatedWith(AwsCredentialAccessKey.class).toInstance("");
    bind(String.class).annotatedWith(AwsCredentialSecretKey.class).toInstance("");
    bind(Region.class)
        .annotatedWith(CoordinatorARegionBinding.class)
        .toInstance(Region.of(cliArgs.getCoordinatorARegion()));
    bind(Region.class)
        .annotatedWith(CoordinatorBRegionBinding.class)
        .toInstance(Region.of(cliArgs.getCoordinatorBRegion()));
    bind(TransactionPhaseManager.class).to(TransactionPhaseManagerImpl.class);
    install(new AwsPbsClientModule());
  }
}
