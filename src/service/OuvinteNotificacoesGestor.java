package service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class OuvinteNotificacoesGestor implements OuvinteEventosAutomoz {
    private final BlockingQueue<String> notificacoes = new LinkedBlockingQueue<String>();

    @Override public void aoOcorrer(EventoAutomoz evento) {
        String mensagem;
        switch (evento.getTipo()) {
            case RESERVA_CRIADA: mensagem = "Novo pedido " + evento.getIdReserva() + ": " + evento.getDescricao(); break;
            case PRAZO_RESERVA_A_TERMINAR: mensagem = "O prazo da reserva " + evento.getIdReserva() + " termina nas proximas 24 horas."; break;
            case RESERVA_EXPIRADA: mensagem = "Reserva " + evento.getIdReserva() + " expirada. " + evento.getDescricao(); break;
            case RESERVA_CANCELADA: mensagem = "Reserva " + evento.getIdReserva() + " cancelada. " + evento.getDescricao(); break;
            case PEDIDO_APROVADO: mensagem = "Pedido " + evento.getIdReserva() + " aprovado."; break;
            case CONTRATO_CRIADO: mensagem = "Contrato " + evento.getDescricao() + " criado para a reserva " + evento.getIdReserva() + "."; break;
            case DEVOLUCAO_REGISTADA: mensagem = "Devolucao registada: " + evento.getDescricao(); break;
            default: mensagem = evento.getDescricao();
        }
        notificacoes.offer(mensagem);
    }

    String seguinte() { return notificacoes.poll(); }
}
