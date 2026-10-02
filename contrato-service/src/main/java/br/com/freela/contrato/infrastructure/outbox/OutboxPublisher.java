package br.com.freela.contrato.infrastructure.outbox;

import br.com.freela.contrato.infrastructure.menssaging.ContratoEventoDTO;
import br.com.freela.contrato.infrastructure.menssaging.ContratoProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final ContratoProducer producer;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository repository,
                           ContratoProducer producer,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publicar() {
        var pendentes = repository.findByStatusOrderByCreatedAtAsc("PENDENTE");
        for (var outbox : pendentes) {
            try {
                var dto = objectMapper.readValue(outbox.getPayload(), ContratoEventoDTO.class);
                try {
                    MDC.put("correlationId", dto.getCorrelationId());
                    producer.enviar(dto);
                    outbox.publicado();
                    repository.save(outbox);
                    log.info("outbox.publicado outboxId={} aggregateId={}", outbox.getId(), outbox.getAggregateId());
                } finally {
                    MDC.remove("correlationId");
                }
            } catch (Exception ex) {
                log.error("outbox.erro outboxId={} erro={}", outbox.getId(), ex.getMessage(), ex);
            }
        }
    }
}
