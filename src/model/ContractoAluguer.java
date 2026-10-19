package model;

import java.time.LocalDateTime;

public class ContractoAluguer extends Contracto {
    private int numeroMeses;
    private double mensalidadeFixa;
    private LocalDateTime dataFim;

    public ContractoAluguer(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataAssinatura,
                            String assinaturaDigitalBase64, byte[] imagemAssinatura, int numeroMeses,
                            LocalDateTime dataFim, double mensalidadeFixa) {
        super(id, cliente, veiculo, dataAssinatura, assinaturaDigitalBase64, imagemAssinatura);
        this.numeroMeses = numeroMeses;
        this.dataFim = dataFim;
        this.mensalidadeFixa = mensalidadeFixa;
    }

    public int getNumeroMeses() { return numeroMeses; }
    public double getMensalidadeFixa() { return mensalidadeFixa; }
    public LocalDateTime getDataFim() { return dataFim; }
}
