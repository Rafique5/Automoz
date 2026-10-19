package model;

public class Cliente extends Pessoa{
    private String cartaConducao;
    private TipoVeiculo intencao;

    public Cliente(String id, String nome, String email, String contacto, String cartaConducao, TipoVeiculo intencao) {
        super(id, nome, email, contacto);
        this.cartaConducao = cartaConducao;
        this.intencao = intencao;
    }

    public String getCartaConducao() {
        return cartaConducao;
    }

    public TipoVeiculo getIntencao() { return intencao; }
}
