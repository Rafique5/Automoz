package service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

final class BarramentoEventos implements AutoCloseable {
    private final List<OuvinteEventosAutomoz> ouvintes = new CopyOnWriteArrayList<OuvinteEventosAutomoz>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable tarefa) {
            Thread thread = new Thread(tarefa, "automoz-eventos");
            thread.setDaemon(true);
            return thread;
        }
    });

    void subscrever(OuvinteEventosAutomoz ouvinte) {
        if (ouvinte == null) throw new IllegalArgumentException("O ouvinte e obrigatorio.");
        ouvintes.add(ouvinte);
    }

    void publicar(final EventoAutomoz evento) {
        if (evento == null) throw new IllegalArgumentException("O evento e obrigatorio.");
        executor.execute(new Runnable() {
            @Override public void run() {
                for (OuvinteEventosAutomoz ouvinte : ouvintes) {
                    try { ouvinte.aoOcorrer(evento); }
                    catch (RuntimeException e) {
                        System.err.println("Falha ao processar evento " + evento.getTipo() + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    @Override public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
