package model;

public enum EstadoVeiculo {
    DISPONIVEL("Disponivel"), RESERVADO("Reservado"), ALUGADO("Alugado"), VENDIDO("Vendido");
    private final String descricao;
    EstadoVeiculo(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
