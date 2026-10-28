package model;

/** Opções de duração suportadas para um contrato de aluguer. */
public enum PeriodoAluguer {
    UM_DIA("1 dia", 1),
    DOIS_DIAS("2 dias", 2),
    SEMANAL("Semanal", 7),
    MENSAL("Mensal", 30);

    private final String descricao;
    private final int diasCobrados;

    PeriodoAluguer(String descricao, int diasCobrados) {
        this.descricao = descricao;
        this.diasCobrados = diasCobrados;
    }

    public String getDescricao() { return descricao; }
    public int getDiasCobrados() { return diasCobrados; }
}
