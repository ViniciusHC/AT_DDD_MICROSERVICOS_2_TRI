package br.com.freela.notificacao.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ContratoEventoDTO {
    private UUID eventId;
    private String tipoEvento;
    private UUID contratoId;
    private UUID clienteId;
    private UUID freelancerId;
    private String titulo;
    private BigDecimal valor;
    private String status;
    private Instant ocorridoEm;
    private String correlationId;

    public ContratoEventoDTO() {
    }

    public ContratoEventoDTO(UUID eventId, String tipoEvento, UUID contratoId, UUID clienteId,
                             UUID freelancerId, String titulo, BigDecimal valor,
                             String status, Instant ocorridoEm, String correlationId) {
        this.eventId = eventId;
        this.tipoEvento = tipoEvento;
        this.contratoId = contratoId;
        this.clienteId = clienteId;
        this.freelancerId = freelancerId;
        this.titulo = titulo;
        this.valor = valor;
        this.status = status;
        this.ocorridoEm = ocorridoEm;
        this.correlationId = correlationId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public void setContratoId(UUID contratoId) {
        this.contratoId = contratoId;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public void setClienteId(UUID clienteId) {
        this.clienteId = clienteId;
    }

    public UUID getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(UUID freelancerId) {
        this.freelancerId = freelancerId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public void setOcorridoEm(Instant ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
