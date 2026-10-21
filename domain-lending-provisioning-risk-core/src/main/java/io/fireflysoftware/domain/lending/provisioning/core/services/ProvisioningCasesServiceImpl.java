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

package io.fireflysoftware.domain.lending.provisioning.core.services;

import io.fireflysoftware.core.lending.provisioning.sdk.model.ProvisioningCaseDTO;
import io.fireflysoftware.domain.lending.provisioning.core.commands.CreateProvisioningCaseCommand;
import io.fireflysoftware.domain.lending.provisioning.core.commands.GetProvisioningCaseQuery;
import io.fireflysoftware.domain.lending.provisioning.core.commands.RemoveProvisioningCaseCommand;
import io.fireflysoftware.domain.lending.provisioning.core.commands.UpdateProvisioningCaseCommand;
import io.fireflysoftware.domain.lending.provisioning.core.commands.UpdateProvisioningCaseStatusCommand;
import io.fireflysoftware.domain.lending.provisioning.core.lifecycle.ProvisioningCaseLifecycle;
import io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the provisioning case sagas and reads through the query bus.
 */
@Service
public class ProvisioningCasesServiceImpl implements ProvisioningCasesService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public ProvisioningCasesServiceImpl(SagaEngine sagaEngine, QueryBus queryBus, CommandBus commandBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    @Override
    public Mono<ProvisioningCaseDTO> create(CreateProvisioningCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_REGISTER, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_REGISTER, ProvisioningCaseDTO.class));
    }

    @Override
    public Mono<ProvisioningCaseDTO> updateStatus(UpdateProvisioningCaseStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_RETRIEVE, new GetProvisioningCaseQuery(command.provisioningCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_UPDATE_STATUS, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_UPDATE_STATUS, ProvisioningCaseDTO.class));
    }

    @Override
    public Mono<ProvisioningCaseDTO> get(UUID provisioningCaseId) {
        return queryBus.query(new GetProvisioningCaseQuery(provisioningCaseId));
    }

    @Override
    public Mono<ProvisioningCaseDTO> update(UpdateProvisioningCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_RETRIEVE_FOR_UPDATE, new GetProvisioningCaseQuery(command.provisioningCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_UPDATE_RECORD, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.SAGA_UPDATE_RECORD, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants.STEP_UPDATE_RECORD, ProvisioningCaseDTO.class));
    }

    @Override
    public Mono<ProvisioningCaseDTO> close(UUID provisioningCaseId, String statusReason) {
        return transition(provisioningCaseId, ProvisioningCaseDTO.ProvisioningStatusEnum.RELEASED, statusReason);
    }

    private Mono<ProvisioningCaseDTO> transition(UUID provisioningCaseId, ProvisioningCaseDTO.ProvisioningStatusEnum target, String statusReason) {
        return get(provisioningCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Provisioning case was not found")))
                .flatMap(current -> {
                    if (!ProvisioningCaseLifecycle.canTransition(current.getProvisioningStatus(), target)) {
                        String from = current.getProvisioningStatus() == null
                                ? "unset"
                                : current.getProvisioningStatus().getValue();
                        return Mono.<ProvisioningCaseDTO>error(new IllegalStateException(
                                "Provisioning case " + provisioningCaseId + " cannot move from " + from + " to " + target.getValue()));
                    }
                    return updateStatus(new UpdateProvisioningCaseStatusCommand(provisioningCaseId, target.getValue(), statusReason));
                });
    }

    @Override
    public Mono<Void> remove(UUID provisioningCaseId) {
        return get(provisioningCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Provisioning case was not found")))
                .flatMap(current -> {
                    if (!ProvisioningCaseLifecycle.canRemove(current.getProvisioningStatus())) {
                        String statusValue = current.getProvisioningStatus() == null
                                ? "unset"
                                : current.getProvisioningStatus().getValue();
                        return Mono.error(new IllegalStateException(
                                "Provisioning case " + provisioningCaseId + " is " + statusValue
                                        + " and cannot be deleted; close it so the case is kept"));
                    }
                    return commandBus.send(new RemoveProvisioningCaseCommand(provisioningCaseId));
                });
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
