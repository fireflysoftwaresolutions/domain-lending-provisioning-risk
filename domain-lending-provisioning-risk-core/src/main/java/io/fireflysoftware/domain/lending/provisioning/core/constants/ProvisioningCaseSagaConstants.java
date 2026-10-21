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

package io.fireflysoftware.domain.lending.provisioning.core.constants;

/**
 * Names shared by the provisioning case sagas, their steps, and the saga engine.
 */
public final class ProvisioningCaseSagaConstants {

    public static final String SAGA_REGISTER = "RegisterProvisioningCaseSaga";
    public static final String SAGA_UPDATE_STATUS = "UpdateProvisioningCaseStatusSaga";
    public static final String SAGA_UPDATE_RECORD = "UpdateProvisioningCaseSaga";

    public static final String STEP_REGISTER = "registerProvisioningCase";
    public static final String STEP_RETRIEVE = "retrieveProvisioningCase";
    public static final String STEP_UPDATE_STATUS = "updateProvisioningCaseStatus";
    public static final String STEP_RETRIEVE_FOR_UPDATE = "retrieveProvisioningCaseForUpdate";
    public static final String STEP_UPDATE_RECORD = "updateProvisioningCase";

    public static final String COMPENSATE_REMOVE = "removeProvisioningCase";
    public static final String COMPENSATE_RESTORE = "restoreProvisioningCaseStatus";
    public static final String COMPENSATE_RESTORE_RECORD = "restoreProvisioningCase";

    public static final String EVENT_REGISTERED = "provisioning-cases.registered";
    public static final String EVENT_RETRIEVED = "provisioning-cases.retrieved";
    public static final String EVENT_STATUS_UPDATED = "provisioning-cases.status.updated";
    public static final String EVENT_RECORD_UPDATED = "provisioning-cases.updated";

    public static final String CTX_RECORD = "provisioning-casesRecord";
    public static final String CTX_PREVIOUS_STATUS = "previousProvisioningCaseStatus";
    public static final String CTX_PREVIOUS_RECORD = "previousProvisioningCaseRecord";

    private ProvisioningCaseSagaConstants() {
    }
}
