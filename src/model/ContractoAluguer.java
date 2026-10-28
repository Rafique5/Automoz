package model;

import java.time.LocalDateTime;

public class ContractoAluguer extends Contracto {
    private PeriodoAluguer periodo;
    private Integer mesesLegados;
    private LocalDateTime dataInicio;
    private double mensalidadeFixa;
    private LocalDateTime dataFim;

    public ContractoAluguer(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataAssinatura,
                            String assinaturaDigitalBase64, byte[] imagemAssinatura, PeriodoAluguer periodo,
                            Integer mesesLegados, LocalDateTime dataInicio, LocalDateTime dataFim, double mensalidadeFixa) {
        super(id, cliente, veiculo, dataAssinatura, assinaturaDigitalBase64, imagemAssinatura);
        this.periodo = periodo;
        this.mesesLegados = mesesLegados;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.mensalidadeFixa = mensalidadeFixa;
    }

    public PeriodoAluguer getPeriodo() { return periodo; }
    public Integer getMesesLegados() { return mesesLegados; }
    public LocalDateTime getDataInicio() { return dataInicio; }
    public double getMensalidadeFixa() { return mensalidadeFixa; }
    public LocalDateTime getDataFim() { return dataFim; }
}
