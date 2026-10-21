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

package io.fireflysoftware.domain.lending.provisioning.web.controllers;

import io.fireflysoftware.core.lending.provisioning.sdk.model.ProvisioningCaseDTO;
import io.fireflysoftware.domain.lending.provisioning.core.commands.CreateProvisioningCaseCommand;
import io.fireflysoftware.domain.lending.provisioning.core.commands.UpdateProvisioningCaseCommand;
import io.fireflysoftware.domain.lending.provisioning.core.commands.UpdateProvisioningCaseStatusCommand;
import io.fireflysoftware.domain.lending.provisioning.core.services.ProvisioningCasesService;
import io.fireflysoftware.domain.lending.provisioning.interfaces.dtos.ReasonRequest;
import io.fireflysoftware.domain.lending.provisioning.interfaces.dtos.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Domain API for provisioning cases.
 */
@RestController
@RequestMapping("/api/v1/provisioning-cases")
@Tag(name = "ProvisioningCases", description = "Domain orchestration for provisioning cases")
public class ProvisioningCasesController {

    private final ProvisioningCasesService service;

    public ProvisioningCasesController(ProvisioningCasesService service) {
        this.service = service;
    }

    @Operation(summary = "Create provisioning case", description = "Runs the register saga against core-lending-provisioning-risk.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProvisioningCaseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProvisioningCaseDTO>> create(@RequestBody ProvisioningCaseDTO body) {
        return service.create(new CreateProvisioningCaseCommand(body))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @Operation(summary = "Get provisioning case", description = "Reads one record from core-lending-provisioning-risk.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProvisioningCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping(value = "/{provisioningCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProvisioningCaseDTO>> get(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID provisioningCaseId) {
        return service.get(provisioningCaseId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update status", description = "Runs the status saga against core-lending-provisioning-risk.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProvisioningCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{provisioningCaseId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProvisioningCaseDTO>> updateStatus(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID provisioningCaseId,
            @RequestBody StatusChangeRequest request) {
        return service.updateStatus(new UpdateProvisioningCaseStatusCommand(provisioningCaseId, request.getStatus(), request.getStatusReason()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update provisioning case", description = "Runs the update saga against core-lending-provisioning-risk. Status is left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProvisioningCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{provisioningCaseId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProvisioningCaseDTO>> update(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID provisioningCaseId,
            @RequestBody ProvisioningCaseDTO body) {
        return service.update(new UpdateProvisioningCaseCommand(provisioningCaseId, body))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Close provisioning case", description = "Closes a provisioning case by writing RELEASED when the provisioning core allows that status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProvisioningCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{provisioningCaseId}/close", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProvisioningCaseDTO>> close(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID provisioningCaseId,
            @RequestBody ReasonRequest request) {
        return service.close(provisioningCaseId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Remove provisioning case", description = "Deletes the record through core-lending-provisioning-risk when that core allows removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @DeleteMapping("/{provisioningCaseId}")
    public Mono<ResponseEntity<Void>> remove(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID provisioningCaseId) {
        return service.remove(provisioningCaseId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
