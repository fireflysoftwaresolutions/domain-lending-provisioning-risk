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
import io.fireflysoftware.domain.lending.provisioning.core.services.ProvisioningCasesService;
import io.fireflysoftware.domain.lending.provisioning.interfaces.dtos.ReasonRequest;
import io.fireflysoftware.domain.lending.provisioning.interfaces.dtos.StatusChangeRequest;
import org.fireflyframework.web.error.config.ErrorHandlingProperties;
import org.fireflyframework.web.error.converter.ExceptionConverterService;
import org.fireflyframework.web.error.service.ErrorResponseNegotiator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Web slice tests for the provisioning case domain API.
 */
@WebFluxTest(ProvisioningCasesController.class)
class ProvisioningCasesControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProvisioningCasesService service;

    @MockitoBean
    private ExceptionConverterService exceptionConverterService;

    @MockitoBean
    private ErrorHandlingProperties errorHandlingProperties;

    @MockitoBean
    private ErrorResponseNegotiator errorResponseNegotiator;

    @Test
    void create_returns201() {
        when(service.create(any())).thenReturn(Mono.just(new ProvisioningCaseDTO()));

        webTestClient.post()
                .uri("/api/v1/provisioning-cases")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ProvisioningCaseDTO())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProvisioningCaseDTO.class);
    }

    @Test
    void get_returns200WhenFound() {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(Mono.just(new ProvisioningCaseDTO(id, null, null)));

        webTestClient.get()
                .uri("/api/v1/provisioning-cases/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProvisioningCaseDTO.class);
    }

    @Test
    void get_returns404WhenMissing() {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/provisioning-cases/{id}", id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateStatus_returns200() {
        UUID id = UUID.randomUUID();
        when(service.updateStatus(any())).thenReturn(Mono.just(new ProvisioningCaseDTO(id, null, null)));
        StatusChangeRequest request = new StatusChangeRequest();
        request.setStatus("RELEASED");
        request.setStatusReason("Caller request");

        webTestClient.put()
                .uri("/api/v1/provisioning-cases/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProvisioningCaseDTO.class);
    }

    @Test
    void update_returns200() {
        UUID id = UUID.randomUUID();
        when(service.update(any())).thenReturn(Mono.just(new ProvisioningCaseDTO(id, null, null)));

        webTestClient.put()
                .uri("/api/v1/provisioning-cases/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ProvisioningCaseDTO())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProvisioningCaseDTO.class);
    }

    @Test
    void close_returns200() {
        UUID id = UUID.randomUUID();
        when(service.close(any(), any())).thenReturn(Mono.just(new ProvisioningCaseDTO()));
        ReasonRequest request = new ReasonRequest();
        request.setStatusReason("Caller request");

        webTestClient.post()
                .uri("/api/v1/provisioning-cases/{id}/close", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProvisioningCaseDTO.class);
    }

    @Test
    void remove_returns204() {
        UUID id = UUID.randomUUID();
        when(service.remove(id)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/provisioning-cases/{id}", id)
                .exchange()
                .expectStatus().isNoContent();
    }
}
