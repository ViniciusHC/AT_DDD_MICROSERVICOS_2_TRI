package br.com.freela.auditoria.infrastructure.messaging;

import br.com.freela.auditoria.AuditoriaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditoriaKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaKafkaConsumer.class);
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public AuditoriaKafkaConsumer(AuditoriaService auditoriaService, ObjectMapper objectMapper) {
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "contratos.eventos", groupId = "auditoria-group")
    public void consumir(ContratoEventoDTO evento) {
        if (evento.getCorrelationId() != null && !evento.getCorrelationId().isBlank()) {
            MDC.put("correlationId", evento.getCorrelationId());
        }

        UUID aggregateId = evento.getContratoId();

        try {
            String payloadJson = objectMapper.writeValueAsString(evento);

            log.info("auditoria.evento.recebido eventId={} tipoEvento={} aggregateId={}",
                    evento.getEventId(), evento.getTipoEvento(), aggregateId);

            auditoriaService.registrar(
                    evento.getEventId(),
                    evento.getContratoId(),
                    evento.getTipoEvento(),
                    evento.getCorrelationId(),
                    payloadJson
            );

            log.info("auditoria.evento.persistido.sucesso eventId={} aggregateId={}",
                    evento.getEventId(), aggregateId);

        } catch (Exception ex) {
            log.error("auditoria.processamento.erro eventId={} aggregateId={} erro={}",
                    evento.getEventId(), aggregateId, ex.getMessage(), ex);
            throw new RuntimeException("Falha ao registrar auditoria", ex);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
