package model;

import java.time.LocalDateTime;

public abstract class Contracto {
    private String id;
    private Cliente cliente;
    private Veiculo veiculo;
    private LocalDateTime dataAssinatura;


    //atributos da assinatura digital
    private String assinaturaDigitalBase64; //hash criptografico de seguranca
    private byte[] imagemAssinatura;

    public Contracto(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataAssinatura, String assinaturaDigitalBase64, byte[] imagemAssinatura) {
        this.id = id;
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.dataAssinatura = dataAssinatura;
        this.assinaturaDigitalBase64 = assinaturaDigitalBase64;
        this.imagemAssinatura = imagemAssinatura;
    }

    public String getId() {
        return id;
    }

    public byte[] getImagemAssinatura() {
        return imagemAssinatura;
    }

    public String getAssinaturaDigitalBase64() {
        return assinaturaDigitalBase64;
    }

    public LocalDateTime getDataAssinatura() {
        return dataAssinatura;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public Cliente getCliente() {
        return cliente;
    }
}
