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
import io.fireflysoftware.domain.lending.provisioning.core.commands.UpdateProvisioningCaseCommand;
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
 * Verifies a field update keeps the current status and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateProvisioningCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateProvisioningCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateProvisioningCaseSaga(commandBus, queryBus);
    }

    @Test
    void update_keepsTheCurrentStatus() {
        UUID id = UUID.randomUUID();
        ProvisioningCaseDTO current = new ProvisioningCaseDTO(id, null, null);
        current.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE);
        ProvisioningCaseDTO incoming = new ProvisioningCaseDTO();
        incoming.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.RELEASED);
        when(commandBus.send(any(ApplyProvisioningCaseStatusCommand.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, ApplyProvisioningCaseStatusCommand.class).record()));

        ExecutionContext ctx = ExecutionContext.forSaga(ProvisioningCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(ProvisioningCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateProvisioningCase(new UpdateProvisioningCaseCommand(id, incoming), ctx))
                .assertNext(saved -> assertThat(saved.getProvisioningStatus()).isEqualTo(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE))
                .verifyComplete();

        ArgumentCaptor<ApplyProvisioningCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyProvisioningCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().provisioningCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getProvisioningStatus()).isEqualTo(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE);
        assertThat(ctx.getVariableAs(ProvisioningCaseSagaConstants.CTX_PREVIOUS_RECORD, ProvisioningCaseDTO.class)).isSameAs(current);
    }

    @Test
    void restore_writesThePreviousRecordBack() {
        UUID id = UUID.randomUUID();
        ProvisioningCaseDTO previous = new ProvisioningCaseDTO(id, null, null);
        previous.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE);
        ProvisioningCaseDTO updated = new ProvisioningCaseDTO(id, null, null);
        updated.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.RELEASED);
        when(commandBus.send(any(ApplyProvisioningCaseStatusCommand.class))).thenReturn(Mono.just(previous));

        ExecutionContext ctx = ExecutionContext.forSaga(ProvisioningCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(ProvisioningCaseSagaConstants.CTX_PREVIOUS_RECORD, previous);

        StepVerifier.create(saga.restoreProvisioningCase(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyProvisioningCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyProvisioningCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record()).isSameAs(previous);
    }
}
