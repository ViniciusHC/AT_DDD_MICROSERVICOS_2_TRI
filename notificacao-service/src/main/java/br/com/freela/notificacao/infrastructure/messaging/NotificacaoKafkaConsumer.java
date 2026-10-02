package br.com.freela.notificacao.infrastructure.messaging;

import br.com.freela.notificacao.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoKafkaConsumer.class);
    private final NotificacaoService notificacaoService;

    public NotificacaoKafkaConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @KafkaListener(topics = "contratos.eventos", groupId = "notificacao-group")
    public void consumir(ContratoEventoDTO evento) {
        if (evento.getCorrelationId() != null && !evento.getCorrelationId().isBlank()) {
            MDC.put("correlationId", evento.getCorrelationId());
        }

        log.info("notificacao.evento.recebido eventId={} tipoEvento={} contratoId={}",
                evento.getEventId(), evento.getTipoEvento(), evento.getContratoId());

        try {
            switch (evento.getTipoEvento()) {
                case "ContratoCriado" -> {
                    log.info("notificacao.processamento.inicio evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getFreelancerId());
                    notificacaoService.registrar(
                            evento.getEventId(),
                            evento.getContratoId(),
                            evento.getFreelancerId(),
                            "CONTRATO_CRIADO",
                            "Novo contrato criado: " + evento.getTitulo()
                    );
                    log.info("notificacao.processamento.sucesso evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getFreelancerId());
                }
                case "EntregaRegistrada" -> {
                    log.info("notificacao.processamento.inicio evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getClienteId());
                    notificacaoService.registrar(
                            evento.getEventId(),
                            evento.getContratoId(),
                            evento.getClienteId(),
                            "ENTREGA_RECEBIDA",
                            "Uma entrega foi registrada para o contrato: " + evento.getTitulo()
                    );
                    log.info("notificacao.processamento.sucesso evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getClienteId());
                }
                case "ContratoConcluido" -> {
                    log.info("notificacao.processamento.inicio evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getFreelancerId());
                    notificacaoService.registrar(
                            evento.getEventId(),
                            evento.getContratoId(),
                            evento.getFreelancerId(),
                            "CONTRATO_CONCLUIDO",
                            "O contrato " + evento.getTitulo() + " foi concluído com sucesso."
                    );
                    log.info("notificacao.processamento.sucesso evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getFreelancerId());
                }
                case "ContratoCancelado" -> {
                    log.info("notificacao.processamento.inicio evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getFreelancerId());
                    notificacaoService.registrar(
                            evento.getEventId(),
                            evento.getContratoId(),
                            evento.getFreelancerId(),
                            "CONTRATO_CANCELADO",
                            "O contrato " + evento.getTitulo() + " foi cancelado."
                    );
                    log.info("notificacao.processamento.sucesso evento={} contratoId={} destinatarioId={}",
                            evento.getTipoEvento(), evento.getContratoId(), evento.getFreelancerId());
                }
            }
        } catch (Exception ex) {
            log.error("notificacao.processamento.erro evento={} contratoId={} erro={}",
                    evento.getTipoEvento(), evento.getContratoId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
