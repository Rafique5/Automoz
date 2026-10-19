package model;

public enum TipoVeiculo {
    VENDA("Venda"), ALUGUER("Aluguer");
    private final String descricao;
    TipoVeiculo(String descricao) { this.descricao = descricao; }
    public String getDescr() { return descricao; }
}
