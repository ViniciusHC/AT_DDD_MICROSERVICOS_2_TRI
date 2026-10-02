package br.com.freela.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private final NotificacaoRepository repository;
    private final EventoProcessadoRepository eventoProcessadoRepository;

    public NotificacaoService(NotificacaoRepository repository, EventoProcessadoRepository eventoProcessadoRepository) {
        this.repository = repository;
        this.eventoProcessadoRepository = eventoProcessadoRepository;
    }

    @Transactional
    public void registrar(UUID eventId, UUID contratoId, UUID destinatarioId, String tipo, String mensagem) {
        if (eventId != null && eventoProcessadoRepository.existsById(eventId)) {
            log.warn("notificacao.evento.duplicado.ignorado eventId={} contratoId={}", eventId, contratoId);
            return;
        }

        log.info("notificacao.registro.inicio eventId={} contratoId={} destinatarioId={} tipo={}", eventId, contratoId, destinatarioId, tipo);

        var n = repository.save(new Notificacao(contratoId, destinatarioId, tipo, mensagem));
        if (eventId != null) {
            eventoProcessadoRepository.save(new EventoProcessado(eventId));
        }

        log.info("notificacao.registro.sucesso notificacaoId={} eventId={} contratoId={} destinatarioId={}", n.id, eventId, contratoId, destinatarioId);
    }
}
