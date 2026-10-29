package service;

import java.time.LocalDateTime;

public final class EventoAutomoz {
    public enum Tipo {
        RESERVA_CRIADA,
        PRAZO_RESERVA_A_TERMINAR,
        RESERVA_EXPIRADA,
        RESERVA_CANCELADA,
        PEDIDO_APROVADO,
        CONTRATO_CRIADO,
        DEVOLUCAO_REGISTADA
    }

    private final Tipo tipo;
    private final String idReserva;
    private final String descricao;
    private final LocalDateTime dataHora;

    public EventoAutomoz(Tipo tipo, String idReserva, String descricao) {
        if (tipo == null) throw new IllegalArgumentException("O tipo do evento e obrigatorio.");
        this.tipo = tipo;
        this.idReserva = idReserva;
        this.descricao = descricao == null ? "" : descricao;
        this.dataHora = LocalDateTime.now();
    }

    public Tipo getTipo() { return tipo; }
    public String getIdReserva() { return idReserva; }
    public String getDescricao() { return descricao; }
    public LocalDateTime getDataHora() { return dataHora; }
}
