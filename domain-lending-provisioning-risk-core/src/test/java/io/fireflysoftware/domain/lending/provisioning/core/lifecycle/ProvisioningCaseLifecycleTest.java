/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.lending.provisioning.core.lifecycle;

import io.fireflysoftware.core.lending.provisioning.sdk.model.ProvisioningCaseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the domain copy of the core lifecycle.
 */
class ProvisioningCaseLifecycleTest {

    @Test
    void allowsTheCoreMove() {
        assertThat(ProvisioningCaseLifecycle.canTransition(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE, ProvisioningCaseDTO.ProvisioningStatusEnum.RELEASED)).isTrue();
    }

    @Test
    void refusesAMissingTarget() {
        assertThat(ProvisioningCaseLifecycle.canTransition(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE, null)).isFalse();
    }

    @Test
    void removeFollowsTheCoreRule() {

        assertThat(ProvisioningCaseLifecycle.canRemove(null)).isTrue();
        assertThat(ProvisioningCaseLifecycle.canRemove(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE)).isTrue();
        assertThat(ProvisioningCaseLifecycle.canRemove(ProvisioningCaseDTO.ProvisioningStatusEnum.RELEASED)).isTrue();
    }
}
