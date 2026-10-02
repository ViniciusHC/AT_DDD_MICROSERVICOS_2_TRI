package br.com.freela.reputacao.infrastructure.messaging;

import br.com.freela.reputacao.ReputacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReputacaoKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReputacaoKafkaConsumer.class);
    private final ReputacaoService reputacaoService;

    public ReputacaoKafkaConsumer(ReputacaoService reputacaoService) {
        this.reputacaoService = reputacaoService;
    }

    @KafkaListener(topics = "contratos.eventos", groupId = "reputacao-group")
    public void consumir(ContratoEventoDTO evento) {
        if (evento.getCorrelationId() != null && !evento.getCorrelationId().isBlank()) {
            MDC.put("correlationId", evento.getCorrelationId());
        }

        log.info("reputacao.evento.recebido eventId={} tipoEvento={} contratoId={} freelancerId={}",
                evento.getEventId(), evento.getTipoEvento(), evento.getContratoId(), evento.getFreelancerId());

        try {
            if ("ContratoConcluido".equalsIgnoreCase(evento.getTipoEvento())) {
                log.info("reputacao.processamento.atualizacao contratoId={} freelancerId={} valor={}",
                        evento.getContratoId(), evento.getFreelancerId(), evento.getValor());

                reputacaoService.registrarContratoConcluido(
                        evento.getEventId(),
                        evento.getContratoId(),
                        evento.getFreelancerId(),
                        evento.getValor()
                );

                log.info("reputacao.processamento.sucesso contratoId={} freelancerId={}",
                        evento.getContratoId(), evento.getFreelancerId());
            }
        } catch (Exception ex) {
            log.error("reputacao.processamento.erro eventId={} contratoId={} erro={}",
                    evento.getEventId(), evento.getContratoId(), ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
