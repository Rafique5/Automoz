package model;

import java.time.LocalDateTime;

public class Reserva{
    private String id;
    private Cliente cliente;
    private Veiculo veiculo;
    private LocalDateTime dataReserva;
    private LocalDateTime dataExpiracao;
    private EstadoReserva estado;
    private LocalDateTime dataInicioUtilizacao;
    private LocalDateTime dataFimUtilizacao;
    private PeriodoAluguer periodo;

    public Reserva(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataReserva, LocalDateTime dataExpiracao,
                   EstadoReserva estado, LocalDateTime dataInicioUtilizacao, LocalDateTime dataFimUtilizacao,
                   PeriodoAluguer periodo) {
        this.id = id;
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.dataReserva = dataReserva;
        this.dataExpiracao = dataExpiracao;
        this.estado = estado;
        this.dataInicioUtilizacao = dataInicioUtilizacao;
        this.dataFimUtilizacao = dataFimUtilizacao;
        this.periodo = periodo;
    }


    public Cliente getCliente() {
        return cliente;
    }

    public String getId() {
        return id;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public LocalDateTime getDataReserva() {
        return dataReserva;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public LocalDateTime getDataInicioUtilizacao() { return dataInicioUtilizacao; }
    public LocalDateTime getDataFimUtilizacao() { return dataFimUtilizacao; }
    public void setDataFimUtilizacao(LocalDateTime dataFimUtilizacao) { this.dataFimUtilizacao = dataFimUtilizacao; }
    public PeriodoAluguer getPeriodo() { return periodo; }

    public void setEstado(EstadoReserva estado) { this.estado = estado; }
}
