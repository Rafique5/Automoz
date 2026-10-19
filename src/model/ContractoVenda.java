package model;

import java.time.LocalDateTime;

public class ContractoVenda extends Contracto{
    public ContractoVenda(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataAssinatura, String assinaturaDigitalBase64, byte[] imagemAssinatura) {
        super(id, cliente, veiculo, dataAssinatura, assinaturaDigitalBase64, imagemAssinatura);
    }

}
