package service;

import model.Cliente;
import model.ContractoVenda;
import model.TipoVeiculo;
import model.Veiculo;

import java.time.LocalDateTime;

public class ContractoVendaService {
    public ContractoVenda criar(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataAssinatura) {
        if (veiculo == null || veiculo.getTipo() != TipoVeiculo.VENDA)
            throw new IllegalArgumentException("O contrato de venda exige um veiculo do tipo venda.");
        if (id == null || id.trim().isEmpty() || cliente == null)
            throw new IllegalArgumentException("Contrato requer identificador e cliente.");
        new ClienteService().validar(cliente);
        new VeiculoService().validar(veiculo);
        return new ContractoVenda(id, cliente, veiculo,
                dataAssinatura == null ? LocalDateTime.now() : dataAssinatura, null, null);
    }

    public double calcularValorTotal(ContractoVenda contrato) {
        if (contrato == null || contrato.getVeiculo() == null)
            throw new IllegalArgumentException("Contrato de venda invalido.");
        new VeiculoService().validar(contrato.getVeiculo());
        return contrato.getVeiculo().getPreco();
    }
}
