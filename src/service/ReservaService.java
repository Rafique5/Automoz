package service;

import model.Cliente;
import model.EstadoReserva;
import model.Reserva;
import model.Veiculo;
import model.PeriodoAluguer;

import java.time.LocalDateTime;

public class ReservaService {
    private final VeiculoService veiculoService = new VeiculoService();

    public Reserva criar(String id, Cliente cliente, Veiculo veiculo, LocalDateTime dataReserva,
                         LocalDateTime inicioUtilizacao, PeriodoAluguer periodo) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Identificador da reserva obrigatorio.");
        if (cliente == null || veiculo == null) throw new IllegalArgumentException("Reserva requer cliente e veiculo.");
        new ClienteService().validar(cliente);
        veiculoService.validar(veiculo);
        if (veiculo.getTipo() == model.TipoVeiculo.ALUGUER) {
            if (cliente.getCartaConducao() == null || cliente.getCartaConducao().trim().isEmpty())
                throw new IllegalStateException("O cliente precisa de carta de conducao para reservar um aluguer.");
            if (inicioUtilizacao == null || periodo == null)
                throw new IllegalArgumentException("Data de inicio e periodo sao obrigatorios para aluguer.");
        } else if (periodo != null || inicioUtilizacao != null) {
            throw new IllegalArgumentException("Data e periodo de utilizacao aplicam-se apenas a alugueres.");
        }
        LocalDateTime inicio = dataReserva == null ? LocalDateTime.now() : dataReserva;
        LocalDateTime fimUtilizacao = null;
        if (periodo != null) {
            switch (periodo) {
                case UM_DIA: fimUtilizacao = inicioUtilizacao.plusDays(1); break;
                case DOIS_DIAS: fimUtilizacao = inicioUtilizacao.plusDays(2); break;
                case SEMANAL: fimUtilizacao = inicioUtilizacao.plusWeeks(1); break;
                case MENSAL: fimUtilizacao = inicioUtilizacao.plusMonths(1); break;
                default: throw new IllegalArgumentException("Periodo de aluguer invalido.");
            }
            if (inicioUtilizacao.isBefore(inicio.toLocalDate().atStartOfDay()))
                throw new IllegalArgumentException("A data de utilizacao nao pode estar no passado.");
        }
        return new Reserva(id, cliente, veiculo, inicio, inicio.plusHours(48), EstadoReserva.ACTIVA,
                inicioUtilizacao, fimUtilizacao, periodo);
    }

    public boolean expirarSeNecessario(Reserva reserva) {
        validar(reserva);
        if (reserva.getEstado() == EstadoReserva.ACTIVA && !LocalDateTime.now().isBefore(reserva.getDataExpiracao())) {
            reserva.setEstado(EstadoReserva.EXPIRADA);
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
    }

    public void validar(Reserva reserva) {
        if (reserva == null || reserva.getId() == null || reserva.getId().trim().isEmpty() ||
                reserva.getCliente() == null || reserva.getVeiculo() == null || reserva.getEstado() == null ||
                reserva.getDataReserva() == null || reserva.getDataExpiracao() == null)
            throw new IllegalArgumentException("Dados da reserva invalidos.");
        if (!reserva.getDataExpiracao().isAfter(reserva.getDataReserva()))
            throw new IllegalArgumentException("A expiracao deve ser posterior a data da reserva.");
        if (reserva.getVeiculo().getTipo() == model.TipoVeiculo.ALUGUER &&
                (reserva.getDataInicioUtilizacao() == null || reserva.getDataFimUtilizacao() == null ||
                        !reserva.getDataFimUtilizacao().isAfter(reserva.getDataInicioUtilizacao())))
            throw new IllegalArgumentException("Intervalo de aluguer invalido.");
    }
}
