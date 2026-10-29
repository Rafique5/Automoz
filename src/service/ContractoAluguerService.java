package service;

import model.Cliente;
import model.ContractoAluguer;
import model.TipoVeiculo;
import model.Veiculo;
import model.PeriodoAluguer;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ContractoAluguerService {
    public ContractoAluguer criar(String id, Cliente cliente, Veiculo veiculo, PeriodoAluguer periodo,
                                  LocalDateTime dataInicio, double mensalidade, LocalDateTime dataAssinatura) {
        if (veiculo == null || veiculo.getTipo() != TipoVeiculo.ALUGUER)
            throw new IllegalArgumentException("O contrato de aluguer exige um veiculo do tipo aluguer.");
        if (periodo == null) throw new IllegalArgumentException("O periodo do aluguer e obrigatorio.");
        if (mensalidade <= 0 || Double.isNaN(mensalidade) || Double.isInfinite(mensalidade))
            throw new IllegalArgumentException("A mensalidade deve ser maior que zero.");
        validarBase(id, cliente, veiculo);
        LocalDateTime assinatura = dataAssinatura == null ? LocalDateTime.now() : dataAssinatura;
        LocalDateTime inicio = dataInicio == null ? assinatura : dataInicio;
        LocalDateTime fim;
        switch (periodo) {
            case UM_DIA: fim = inicio.plusDays(1); break;
            case DOIS_DIAS: fim = inicio.plusDays(2); break;
            case SEMANAL: fim = inicio.plusWeeks(1); break;
            case MENSAL: fim = inicio.plusMonths(1); break;
            default: throw new IllegalArgumentException("Periodo de aluguer nao suportado.");
        }
        return new ContractoAluguer(id, cliente, veiculo, assinatura, null, null, periodo, null, inicio, fim, mensalidade);
    }

    public double calcularValorTotal(ContractoAluguer contrato) {
        validar(contrato);
        if (contrato.getMesesLegados() != null)
            return contrato.getMensalidadeFixa() * contrato.getMesesLegados();
        return BigDecimal.valueOf(contrato.getMensalidadeFixa())
                .multiply(BigDecimal.valueOf(contrato.getPeriodo().getDiasCobrados()))
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP).doubleValue();
    }

    public boolean estaExpirado(ContractoAluguer contrato, LocalDateTime dataReferencia) {
        validar(contrato);
        LocalDateTime referencia = dataReferencia == null ? LocalDateTime.now() : dataReferencia;
        return !referencia.isBefore(contrato.getDataFim());
    }

    public void validar(ContractoAluguer contrato) {
        if (contrato == null) throw new IllegalArgumentException("Contrato de aluguer obrigatorio.");
        if (contrato.getMesesLegados() == null && (contrato.getPeriodo() == null || contrato.getDataInicio() == null))
            throw new IllegalArgumentException("Periodo e data de inicio do aluguer sao obrigatorios.");
        if (contrato.getMensalidadeFixa() <= 0 || Double.isNaN(contrato.getMensalidadeFixa()) ||
                Double.isInfinite(contrato.getMensalidadeFixa()) || contrato.getDataFim() == null)
            throw new IllegalArgumentException("Dados do contrato de aluguer invalidos.");
        validarBase(contrato.getId(), contrato.getCliente(), contrato.getVeiculo());
    }

    private void validarBase(String id, Cliente cliente, Veiculo veiculo) {
        if (id == null || id.trim().isEmpty() || cliente == null || veiculo == null)
            throw new IllegalArgumentException("Contrato requer identificador, cliente e veiculo.");
        new ClienteService().validar(cliente);
        new VeiculoService().validar(veiculo);
    }
}
