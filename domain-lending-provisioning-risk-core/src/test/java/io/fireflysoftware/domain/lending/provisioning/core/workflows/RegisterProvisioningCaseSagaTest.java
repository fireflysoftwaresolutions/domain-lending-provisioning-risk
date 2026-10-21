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
import io.fireflysoftware.domain.lending.provisioning.core.commands.CreateProvisioningCaseCommand;
import io.fireflysoftware.domain.lending.provisioning.core.commands.RemoveProvisioningCaseCommand;
import io.fireflysoftware.domain.lending.provisioning.core.constants.ProvisioningCaseSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterProvisioningCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterProvisioningCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterProvisioningCaseSaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        ProvisioningCaseDTO created = new ProvisioningCaseDTO(id, null, null);
        created.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE);
        when(commandBus.send(any(CreateProvisioningCaseCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(ProvisioningCaseSagaConstants.SAGA_REGISTER, "test");
        ProvisioningCaseDTO request = new ProvisioningCaseDTO();
        request.setProvisioningStatus(ProvisioningCaseDTO.ProvisioningStatusEnum.ACTIVE);

        StepVerifier.create(saga.registerProvisioningCase(new CreateProvisioningCaseCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(ProvisioningCaseSagaConstants.CTX_RECORD, ProvisioningCaseDTO.class).getProvisioningCaseId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveProvisioningCaseCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removeProvisioningCase(new ProvisioningCaseDTO(id, null, null)))
                .verifyComplete();

        ArgumentCaptor<RemoveProvisioningCaseCommand> captor = ArgumentCaptor.forClass(RemoveProvisioningCaseCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().provisioningCaseId()).isEqualTo(id);
    }

}
