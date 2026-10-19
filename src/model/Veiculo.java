package model;

public class Veiculo {

    private int id;
    private String marca;
    private String modelo;
    private double preco;
    private TipoVeiculo tipo;
    private EstadoVeiculo estado;

    public Veiculo(int id, String marca, String modelo, double preco, TipoVeiculo tipo, EstadoVeiculo estado) {
        this.id = id;
        this.marca = marca;
        this.modelo = modelo;
        this.preco = preco;
        this.tipo = tipo;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public double getPreco() {
        return preco;
    }

    public TipoVeiculo getTipo() {
        return tipo;
    }

    public EstadoVeiculo getEstado() {
        return estado;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public void setTipo(TipoVeiculo tipo) {
        this.tipo = tipo;
    }

    public void setEstado(EstadoVeiculo estado) { this.estado = estado; }
}
