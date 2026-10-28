package service;

import model.Contracto;
import model.ContractoAluguer;
import model.ContractoVenda;
import model.Reserva;
import model.TipoVeiculo;
import model.Veiculo;
import model.PeriodoAluguer;

import java.time.LocalDateTime;

public class ContractoService {
    private final ContractoVendaService vendaService = new ContractoVendaService();
    private final ContractoAluguerService aluguerService = new ContractoAluguerService();
    private final ReservaService reservaService = new ReservaService();
    private final VeiculoService veiculoService = new VeiculoService();

    public Contracto formalizar(String id, Reserva reserva, PeriodoAluguer periodo, LocalDateTime dataInicio) {
        if (reserva == null) throw new IllegalArgumentException("Reserva obrigatoria para formalizar contrato.");
        Veiculo veiculo = reserva.getVeiculo();
        Contracto contrato;
        if (veiculo.getTipo() == TipoVeiculo.VENDA) {
            contrato = vendaService.criar(id, reserva.getCliente(), veiculo, LocalDateTime.now());
        } else {
            contrato = aluguerService.criar(id, reserva.getCliente(), veiculo, periodo, dataInicio,
                    veiculo.getPreco(), LocalDateTime.now());
        }
        reservaService.confirmar(reserva);
        if (veiculo.getTipo() == TipoVeiculo.VENDA) veiculoService.marcarComoVendido(veiculo);
        else veiculoService.marcarComoAlugado(veiculo);
        return contrato;
    }

    public double calcularValorTotal(Contracto contrato) {
        if (contrato instanceof ContractoVenda)
            return vendaService.calcularValorTotal((ContractoVenda) contrato);
        if (contrato instanceof ContractoAluguer)
            return aluguerService.calcularValorTotal((ContractoAluguer) contrato);
        throw new IllegalArgumentException("Tipo de contrato nao suportado.");
    }
}
