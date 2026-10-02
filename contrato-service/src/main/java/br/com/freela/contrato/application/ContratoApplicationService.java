package br.com.freela.contrato.application;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.repository.ContratoRepository;
import br.com.freela.contrato.domain.shared.DomainEvent;
import br.com.freela.contrato.infrastructure.menssaging.ContratoEventoDTO;
import br.com.freela.contrato.infrastructure.outbox.OutboxEvent;
import br.com.freela.contrato.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContratoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ContratoApplicationService.class);
    private final ContratoRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ContratoApplicationService(ContratoRepository repository,
                                      OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Contrato criar(CriarContratoCommand cmd) {
        log.info("contrato.criacao.inicio clienteId={} freelancerId={} titulo={} valor={}",
                cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());

        Contrato contrato = Contrato.criar(cmd.clienteId(), cmd.freelancerId(), cmd.titulo(), cmd.valor());
        Contrato salvo = repository.salvar(contrato);

        registrarEventosNoOutbox(contrato, salvo);

        log.info("contrato.criacao.sucesso contratoId={} clienteId={} freelancerId={} status={}",
                salvo.id(), salvo.clienteId(), salvo.freelancerId(), salvo.status());
        return salvo;
    }

    @Transactional
    public Contrato registrarEntrega(UUID id) {
        log.info("contrato.entrega.inicio contratoId={}", id);

        Contrato contrato = buscar(id);
        contrato.registrarEntrega();
        Contrato salvo = repository.salvar(contrato);

        registrarEventosNoOutbox(contrato, salvo);

        log.info("contrato.entrega.sucesso contratoId={} status={}", salvo.id(), salvo.status());
        return salvo;
    }

    @Transactional
    public Contrato concluir(UUID id) {
        log.info("contrato.conclusao.inicio contratoId={}", id);

        Contrato contrato = buscar(id);
        contrato.concluir();
        Contrato salvo = repository.salvar(contrato);

        registrarEventosNoOutbox(contrato, salvo);

        log.info("contrato.conclusao.sucesso contratoId={} status={}", salvo.id(), salvo.status());
        return salvo;
    }

    @Transactional
    public Contrato cancelar(UUID id) {
        log.info("contrato.cancelamento.inicio contratoId={}", id);

        Contrato contrato = buscar(id);
        contrato.cancelar();
        Contrato salvo = repository.salvar(contrato);

        registrarEventosNoOutbox(contrato, salvo);

        log.info("contrato.cancelamento.sucesso contratoId={} status={}", salvo.id(), salvo.status());
        return salvo;
    }

    private void registrarEventosNoOutbox(Contrato contrato, Contrato salvo) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        for (DomainEvent event : contrato.pullDomainEvents()) {
            log.info("contrato.evento.outbox.registro contratoId={} eventId={} eventType={} occurredAt={}",
                    salvo.id(), event.eventId(), event.eventType(), event.occurredAt());

            ContratoEventoDTO dto = new ContratoEventoDTO(
                    event.eventId(),
                    event.eventType(),
                    salvo.id(),
                    salvo.clienteId(),
                    salvo.freelancerId(),
                    salvo.titulo(),
                    salvo.valor(),
                    salvo.status().name(),
                    event.occurredAt(),
                    correlationId
            );

            try {
                String payloadJson = objectMapper.writeValueAsString(dto);
                outboxEventRepository.save(new OutboxEvent(salvo.id(), payloadJson));
                log.info("contrato.evento.outbox.salvo aggregateId={} eventType={}", salvo.id(), event.eventType());
            } catch (Exception ex) {
                log.error("contrato.evento.outbox.erro eventId={} aggregateId={} erro={}",
                        event.eventId(), salvo.id(), ex.getMessage(), ex);
                throw new RuntimeException("Falha ao serializar evento para outbox", ex);
            }
        }
    }

    @Transactional(readOnly = true)
    public Contrato buscar(UUID id) {
        log.info("contrato.busca.inicio contratoId={}", id);
        var contrato = repository.buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + id));
        log.info("contrato.busca.sucesso contratoId={} status={}", id, contrato.status());
        return contrato;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listar() {
        log.info("contrato.listagem.inicio");
        var contratos = repository.listar();
        log.info("contrato.listagem.sucesso quantidade={}", contratos.size());
        return contratos;
    }
}
