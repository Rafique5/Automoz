package model;

public class Cliente extends Pessoa{
    private String cartaConducao;

    public Cliente(String id, String nome, String email, String contacto, String cartaConducao) {
        super(id, nome, email, contacto);
        this.cartaConducao = cartaConducao;
    }

    public String getCartaConducao() {
        return cartaConducao;
    }

}
