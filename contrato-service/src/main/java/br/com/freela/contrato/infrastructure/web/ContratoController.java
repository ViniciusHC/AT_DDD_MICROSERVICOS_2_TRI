package br.com.freela.contrato.infrastructure.web;

import br.com.freela.contrato.application.ContratoApplicationService;
import br.com.freela.contrato.application.CriarContratoCommand;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {
    private static final Logger log = LoggerFactory.getLogger(ContratoController.class);
    private final ContratoApplicationService service;
    public ContratoController(ContratoApplicationService service){this.service=service;}

    private String setupCorrelationId(String headerCorrelationId) {
        String correlationId;
        if (headerCorrelationId != null && !headerCorrelationId.isBlank()) {
            correlationId = headerCorrelationId;
        } else {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        return correlationId;
    }

    private void clearCorrelationId() {
        MDC.remove("correlationId");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContratoResponse criar(@RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
                                  @Valid @RequestBody CriarContratoRequest request) {
        String correlationId = setupCorrelationId(correlationIdHeader);
        try {
            log.info("http.contrato.criar clienteId={} freelancerId={} titulo={}",
                    request.clienteId(), request.freelancerId(), request.titulo());

            var c = service.criar(new CriarContratoCommand(request.clienteId(), request.freelancerId(), request.titulo(), request.valor()));

            log.info("http.contrato.criar.response contratoId={} status={}", c.id(), c.status());
            return ContratoResponse.from(c);
        } finally {
            clearCorrelationId();
        }
    }

    @GetMapping("/{id}")
    public ContratoResponse buscar(@RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
                                   @PathVariable("id") UUID id) {
        String correlationId = setupCorrelationId(correlationIdHeader);
        try {
            log.info("http.contrato.buscar correlationId={} contratoId={}", correlationId, id);
            return ContratoResponse.from(service.buscar(id));
        } finally {
            clearCorrelationId();
        }
    }

    @GetMapping
    public List<ContratoResponse> listar(@RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader) {
        String correlationId = setupCorrelationId(correlationIdHeader);
        try {
            log.info("http.contrato.listar correlationId={}", correlationId);
            return service.listar().stream().map(ContratoResponse::from).toList();
        } finally {
            clearCorrelationId();
        }
    }

    @PostMapping("/{id}/entregas")
    public ContratoResponse registrarEntrega(@RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
                                             @PathVariable("id") UUID id) {
        String correlationId = setupCorrelationId(correlationIdHeader);
        try {
            log.info("http.contrato.entrega correlationId={} contratoId={}", correlationId, id);
            var c = service.registrarEntrega(id);
            log.info("http.contrato.entrega.response correlationId={} contratoId={} status={}", correlationId, c.id(), c.status());
            return ContratoResponse.from(c);
        } finally {
            clearCorrelationId();
        }
    }

    @PostMapping("/{id}/concluir")
    public ContratoResponse concluir(@RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
                                     @PathVariable("id") UUID id) {
        String correlationId = setupCorrelationId(correlationIdHeader);
        try {
            log.info("http.contrato.concluir correlationId={} contratoId={}", correlationId, id);
            var c = service.concluir(id);
            log.info("http.contrato.concluir.response correlationId={} contratoId={} status={}", correlationId, c.id(), c.status());
            return ContratoResponse.from(c);
        } finally {
            clearCorrelationId();
        }
    }

    @PostMapping("/{id}/cancelar")
    public ContratoResponse cancelar(@RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
                                     @PathVariable("id") UUID id) {
        String correlationId = setupCorrelationId(correlationIdHeader);
        try {
            log.info("http.contrato.cancelar correlationId={} contratoId={}", correlationId, id);
            var c = service.cancelar(id);
            log.info("http.contrato.cancelar.response correlationId={} contratoId={} status={}", correlationId, c.id(), c.status());
            return ContratoResponse.from(c);
        } finally {
            clearCorrelationId();
        }
    }

}
