package br.com.freela.contrato.domain.event;

import br.com.freela.contrato.domain.model.Contrato;
import br.com.freela.contrato.domain.shared.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EntregaRegistrada(UUID eventId, Instant occurredAt, UUID contratoId,
                                UUID clienteId, UUID freelancerId, String titulo, BigDecimal valor)
        implements DomainEvent {

    public static EntregaRegistrada novo(Contrato e) {
        return new EntregaRegistrada(UUID.randomUUID(), Instant.now(), e.id(), e.clienteId(), e.freelancerId(), e.titulo(), e.valor());
    }

    @Override public String eventType() { return "EntregaRegistrada"; }
}
