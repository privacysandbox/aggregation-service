/*
 * Copyright 2022 Google LLC
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

package com.google.aggregate.privacy.budgeting.converter;

import com.google.aggregate.privacy.budgeting.model.PrivacyBudgetKey;
import com.google.common.base.Converter;

/**
 * Converts between the wire format {@link
 * com.google.aggregate.privacy.budgeting.model.PrivacyBudgetKey} and storage format {@link
 * com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey}.
 */
public abstract class PrivacyBudgetKeyConverter
    extends Converter<
        PrivacyBudgetKey, com.google.aggregate.privacy.budgeting.storage.PrivacyBudgetKey> {}
