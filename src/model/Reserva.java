package model;

import java.time.LocalDateTime;

public class Reserva{
    private String id;
    private Cliente cliente;
    private Veiculo veiculo;
    private LocalDateTime dataReserva;
    private LocalDateTime dataExpiracao;
    private EstadoReserva estado;

    public Reserva(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataReserva, LocalDateTime dataExpiracao, EstadoReserva estado) {
        this.id = id;
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.dataReserva = dataReserva;
        this.dataExpiracao = dataExpiracao;
        this.estado = estado;
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

    public void setEstado(EstadoReserva estado) { this.estado = estado; }
}
