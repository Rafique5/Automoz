package model;

public enum EstadoReserva {
    ACTIVA("Activa"), EXPIRADA("Expirada"), CONFIRMADA("Confirmada"), CANCELADA("Cancelada");
    private final String descricao;
    EstadoReserva(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
