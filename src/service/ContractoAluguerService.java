package service;

import model.Cliente;
import model.ContractoAluguer;
import model.TipoVeiculo;
import model.Veiculo;

import java.time.LocalDateTime;

public class ContractoAluguerService {
    public ContractoAluguer criar(String id, Cliente cliente, Veiculo veiculo, int numeroMeses,
                                  double mensalidade, LocalDateTime dataAssinatura) {
        if (veiculo == null || veiculo.getTipo() != TipoVeiculo.ALUGUER)
            throw new IllegalArgumentException("O contrato de aluguer exige um veiculo do tipo aluguer.");
        if (numeroMeses < 1 || numeroMeses > 6)
            throw new IllegalArgumentException("A duracao do aluguer deve ser entre 1 e 6 meses.");
        if (mensalidade <= 0 || Double.isNaN(mensalidade) || Double.isInfinite(mensalidade))
            throw new IllegalArgumentException("A mensalidade deve ser maior que zero.");
        validarBase(id, cliente, veiculo);
        LocalDateTime assinatura = dataAssinatura == null ? LocalDateTime.now() : dataAssinatura;
        return new ContractoAluguer(id, cliente, veiculo, assinatura, null, null, numeroMeses,
                assinatura.plusMonths(numeroMeses), mensalidade);
    }

    public double calcularValorTotal(ContractoAluguer contrato) {
        validar(contrato);
        return contrato.getMensalidadeFixa() * contrato.getNumeroMeses();
    }

    public boolean estaExpirado(ContractoAluguer contrato, LocalDateTime dataReferencia) {
        validar(contrato);
        LocalDateTime referencia = dataReferencia == null ? LocalDateTime.now() : dataReferencia;
        return !referencia.isBefore(contrato.getDataFim());
    }

    public void validar(ContractoAluguer contrato) {
        if (contrato == null) throw new IllegalArgumentException("Contrato de aluguer obrigatorio.");
        if (contrato.getNumeroMeses() < 1 || contrato.getNumeroMeses() > 6)
            throw new IllegalArgumentException("A duracao do aluguer deve ser entre 1 e 6 meses.");
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
