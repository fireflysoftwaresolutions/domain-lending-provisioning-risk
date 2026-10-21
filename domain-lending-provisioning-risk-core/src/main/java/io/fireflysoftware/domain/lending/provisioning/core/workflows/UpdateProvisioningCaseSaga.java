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

package io.fireflysoftware.domain.lending.provisioning.core.workflows;

import io.fireflysoftware.core.lending.provisioning.sdk.model.ProvisioningCaseDTO;
import io.fireflysoftware.domain.lending.provisioning.core.commands.ApplyProvisioningCaseStatusCommand;
import io.fireflysoftware.domain.lending.provisioning.core.commands.GetProvisioningCaseQuery;
import io.fireflysoftware.domain.lending.provisioning.core.commands.UpdateProvisioningCaseCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.COMPENSATE_RESTORE_RECORD;
import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.CTX_PREVIOUS_RECORD;
import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.CTX_RECORD;
import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.EVENT_RECORD_UPDATED;
import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.EVENT_RETRIEVED;
import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.SAGA_UPDATE_RECORD;
import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_RETRIEVE_FOR_UPDATE;
import static io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_UPDATE_RECORD;

/**
 * Updates a provisioning case on {@code core-lending-provisioning-risk} and compensates by writing the previous record back.
 * Status stays on the status saga.
 */
@Saga(name = SAGA_UPDATE_RECORD)
@Service
public class UpdateProvisioningCaseSaga {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public UpdateProvisioningCaseSaga(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_RETRIEVE_FOR_UPDATE)
    @StepEvent(type = EVENT_RETRIEVED)
    public Mono<ProvisioningCaseDTO> retrieveProvisioningCaseForUpdate(GetProvisioningCaseQuery query, ExecutionContext ctx) {
        return queryBus.query(query)
                .doOnNext(record -> ctx.putVariable(CTX_RECORD, record));
    }

    @SagaStep(id = STEP_UPDATE_RECORD, dependsOn = STEP_RETRIEVE_FOR_UPDATE, compensate = COMPENSATE_RESTORE_RECORD)
    @StepEvent(type = EVENT_RECORD_UPDATED)
    public Mono<ProvisioningCaseDTO> updateProvisioningCase(UpdateProvisioningCaseCommand command, ExecutionContext ctx) {
        ProvisioningCaseDTO current = ctx.getVariableAs(CTX_RECORD, ProvisioningCaseDTO.class);
        if (current == null || current.getProvisioningCaseId() == null) {
            return Mono.error(new IllegalStateException("Provisioning case was not found"));
        }
        ProvisioningCaseDTO incoming = command.record();
        if (incoming == null) {
            return Mono.error(new IllegalArgumentException("Provisioning case is required"));
        }
        if (current.getProvisioningStatus() != null) {
            incoming.setProvisioningStatus(current.getProvisioningStatus());
        }
        ctx.putVariable(CTX_PREVIOUS_RECORD, current);
        return commandBus.send(new ApplyProvisioningCaseStatusCommand(current.getProvisioningCaseId(), incoming))
                .doOnNext(updated -> ctx.putVariable(CTX_RECORD, updated));
    }

    public Mono<Void> restoreProvisioningCase(ProvisioningCaseDTO updated, ExecutionContext ctx) {
        ProvisioningCaseDTO previous = ctx.getVariableAs(CTX_PREVIOUS_RECORD, ProvisioningCaseDTO.class);
        if (previous == null || previous.getProvisioningCaseId() == null) {
            return Mono.empty();
        }
        return commandBus.send(new ApplyProvisioningCaseStatusCommand(previous.getProvisioningCaseId(), previous)).then();
    }
}
