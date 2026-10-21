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

package io.fireflysoftware.domain.lending.provisioning.core.handlers;

import io.fireflysoftware.core.lending.provisioning.sdk.api.ProvisioningCaseApi;
import io.fireflysoftware.core.lending.provisioning.sdk.model.ProvisioningCaseDTO;
import io.fireflysoftware.domain.lending.provisioning.core.commands.ApplyProvisioningCaseStatusCommand;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Updates a provisioning case, including its status, through the core.
 */
@Slf4j
@CommandHandlerComponent
public class ApplyProvisioningCaseStatusHandler extends CommandHandler<ApplyProvisioningCaseStatusCommand, ProvisioningCaseDTO> {

    private final ProvisioningCaseApi api;

    public ApplyProvisioningCaseStatusHandler(ProvisioningCaseApi api) {
        this.api = api;
    }

    @Override
    protected Mono<ProvisioningCaseDTO> doHandle(ApplyProvisioningCaseStatusCommand command) {
        log.debug("Updating provisioning case status: provisioningCaseId={}", command.provisioningCaseId());
        return api.update(command.provisioningCaseId(), command.record(), UUID.randomUUID().toString());
    }
}
