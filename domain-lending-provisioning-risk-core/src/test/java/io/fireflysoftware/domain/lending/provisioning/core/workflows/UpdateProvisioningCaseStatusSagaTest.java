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
import io.fireflysoftware.domain.lending.provisioning.core.commands.UpdateProvisioningCaseStatusCommand;
import io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateProvisioningCaseStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateProvisioningCaseStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateProvisioningCaseStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        ProvisioningCaseDTO current = new ProvisioningCaseDTO(id, null, null);
        current.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE);
        when(commandBus.send(any(ApplyProvisioningCaseStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(ProvisioningCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(ProvisioningCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateProvisioningCaseStatus(new UpdateProvisioningCaseStatusCommand(id, "RELEASED", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyProvisioningCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyProvisioningCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().provisioningCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getProvisioningStatus()).isEqualTo(ProvisioningCaseDTO.ProvisioningStatusEnum.RELEASED);
        assertThat(ctx.getVariableAs(ProvisioningCaseSagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("ACTIVE");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        ProvisioningCaseDTO updated = new ProvisioningCaseDTO(id, null, null);
        updated.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.RELEASED);
        when(commandBus.send(any(ApplyProvisioningCaseStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(ProvisioningCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(ProvisioningCaseSagaConstants.CTX_PREVIOUS_STATUS, "ACTIVE");

        StepVerifier.create(saga.restoreProvisioningCaseStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyProvisioningCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyProvisioningCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getProvisioningStatus()).isEqualTo(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE);
    }

}
