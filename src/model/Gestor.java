package model;

public class Gestor extends Pessoa{
    public Gestor(String id, String nome, String email, String contacto) {
        super(id, nome, email, contacto);
    }

    public Gestor(int id, String nome, String email, String contacto) {
        this(String.valueOf(id), nome, email, contacto);
    }

    private void aprovarPedidoEspecial(){}
    private double definirValorDanos(){
        return 0;//coloquei esse 0 porque estava a mostrar erro
    }

}
