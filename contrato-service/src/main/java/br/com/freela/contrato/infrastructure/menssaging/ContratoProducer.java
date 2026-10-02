package br.com.freela.contrato.infrastructure.menssaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContratoProducer {
    private static final String TOPICO = "contratos.eventos";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContratoProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviar(ContratoEventoDTO evento) {
        String chave = evento.getContratoId().toString();
        kafkaTemplate.send(TOPICO, chave, evento);
    }
}
