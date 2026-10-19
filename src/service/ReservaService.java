package service;

import model.Cliente;
import model.EstadoReserva;
import model.Reserva;
import model.Veiculo;

import java.time.LocalDateTime;

public class ReservaService {
    private final VeiculoService veiculoService = new VeiculoService();

    public Reserva criar(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataReserva) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Identificador da reserva obrigatorio.");
        if (cliente == null || veiculo == null) throw new IllegalArgumentException("Reserva requer cliente e veiculo.");
        new ClienteService().validar(cliente);
        veiculoService.validar(veiculo);
        if (cliente.getIntencao() != veiculo.getTipo())
            throw new IllegalStateException("O tipo do veiculo nao corresponde a intencao indicada pelo cliente.");
        LocalDateTime inicio = dataReserva == null ? LocalDateTime.now() : dataReserva;
        veiculoService.reservar(veiculo);
        return new Reserva(id, cliente, veiculo, inicio, inicio.plusHours(48), EstadoReserva.ACTIVA);
    }

    public boolean expirarSeNecessario(Reserva reserva) {
        validar(reserva);
        if (reserva.getEstado() == EstadoReserva.ACTIVA && !LocalDateTime.now().isBefore(reserva.getDataExpiracao())) {
            reserva.setEstado(EstadoReserva.EXPIRADA);
            veiculoService.libertar(reserva.getVeiculo());
            return true;
        }
        return false;
    }

    public void confirmar(Reserva reserva) {
        validar(reserva);
        if (reserva.getEstado() != EstadoReserva.ACTIVA)
            throw new IllegalStateException("Apenas reservas activas podem ser confirmadas.");
        if (expirarSeNecessario(reserva)) throw new IllegalStateException("A reserva expirou apos 48 horas.");
        reserva.setEstado(EstadoReserva.CONFIRMADA);
    }

    public void cancelar(Reserva reserva) {
        validar(reserva);
        if (reserva.getEstado() != EstadoReserva.ACTIVA)
            throw new IllegalStateException("Apenas reservas activas podem ser canceladas.");
        reserva.setEstado(EstadoReserva.CANCELADA);
        veiculoService.libertar(reserva.getVeiculo());
    }

    public void validar(Reserva reserva) {
        if (reserva == null || reserva.getId() == null || reserva.getId().trim().isEmpty() ||
                reserva.getCliente() == null || reserva.getVeiculo() == null || reserva.getEstado() == null ||
                reserva.getDataReserva() == null || reserva.getDataExpiracao() == null)
            throw new IllegalArgumentException("Dados da reserva invalidos.");
        if (!reserva.getDataExpiracao().isAfter(reserva.getDataReserva()))
            throw new IllegalArgumentException("A expiracao deve ser posterior a data da reserva.");
    }
}
