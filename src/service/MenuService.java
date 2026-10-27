package service;

import model.Cliente;
import model.Contracto;
import model.Reserva;
import model.TipoVeiculo;
import model.Veiculo;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class MenuService {
    private final Scanner input;
    private final AutomozService automozService;

    public MenuService(Scanner input, AutomozService automozService) {
        if (input == null || automozService == null)
            throw new IllegalArgumentException("Scanner e AutomozService sao obrigatorios.");
        this.input = input;
        this.automozService = automozService;
    }

    public void registarVeiculo() {
        String marca = lerTexto("Marca: ");
        String modelo = lerTexto("Modelo: ");
        TipoVeiculo tipo = lerTipo();
        String promptPreco = tipo == TipoVeiculo.VENDA ? "Preco de venda: " : "Mensalidade: ";
        double preco = lerDouble(promptPreco);
        Veiculo veiculo = automozService.registarVeiculo(marca, modelo, preco, tipo);
        System.out.println("Veiculo registado com ID " + veiculo.getId());
    }

    public void registarCliente() {
        String nome = lerTexto("Nome: ");
        String email = lerTexto("Email: ");
        String contacto = lerContacto();
        if (contacto == null) {
            System.out.println("Registo de cliente cancelado.");
            return;
        }
        TipoVeiculo intencao = lerTipoCliente();
        String cartaConducao = intencao == TipoVeiculo.ALUGUER
                ? lerTexto("Carta de conducao: ") : "";
        Cliente cliente = automozService.registarCliente(nome, email, contacto,
                cartaConducao, intencao);
        System.out.println("Cliente registado com ID " + cliente.getId());
    }

    public void listarVeiculos() {
        if (automozService.listarVeiculos().isEmpty()) { System.out.println("Ainda nao existem veiculos."); return; }
        for (Veiculo v : automozService.listarVeiculos())
            System.out.println("#" + v.getId() + " " + v.getMarca() + " " + v.getModelo() + " | " + v.getTipo().getDescr() +
                    " | " + v.getPreco() + " | " + v.getEstado().getDescricao());
    }

    public void listarClientes() {
        if (automozService.listarClientes().isEmpty()) { System.out.println("Ainda nao existem clientes."); return; }
        for (Cliente c : automozService.listarClientes())
            System.out.println(c.getId() + " " + c.getNome() + " | " + c.getEmail() + " | intencao: " +
                    c.getIntencao().getDescr() + " | carta: " + c.getCartaConducao());
    }

    public void criarReserva() {
        listarVeiculos();
        String idCliente = lerTexto("ID do cliente (ex.: CALG300001 ou CVD780001): ");
        Reserva reserva = automozService.criarReserva(idCliente, lerInteiro("ID do veiculo: "));
        System.out.println("Reserva " + reserva.getId() + " criada; expira em " + reserva.getDataExpiracao());
    }

    /** Lança dois pedidos em paralelo para demonstrar a exclusão mútua na reserva. */
    public void simularReservasConcorrentes() {
        listarVeiculos();
        int idVeiculo = lerInteiro("ID de um veiculo de aluguer disponivel: ");
        final String[] clientes = {
                lerTexto("ID do primeiro cliente de aluguer: "),
                lerTexto("ID do segundo cliente de aluguer: ")
        };
        final int veiculo = idVeiculo;
        final String[] resultados = new String[2];
        final CountDownLatch prontos = new CountDownLatch(2);
        final CountDownLatch iniciar = new CountDownLatch(1);
        Thread[] pedidos = new Thread[2];

        for (int i = 0; i < pedidos.length; i++) {
            final int indice = i;
            pedidos[i] = new Thread(new Runnable() {
                @Override public void run() {
                    prontos.countDown();
                    try {
                        iniciar.await();
                        Reserva reserva = automozService.criarReserva(clientes[indice], veiculo);
                        resultados[indice] = "Reserva criada: " + reserva.getId();
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        resultados[indice] = "Pedido recusado: " + e.getMessage();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        resultados[indice] = "Pedido interrompido.";
                    }
                }
            }, "pedido-reserva-" + (i + 1));
            pedidos[i].start();
        }

        try {
            prontos.await();
            iniciar.countDown();
            for (Thread pedido : pedidos) pedido.join();
        } catch (InterruptedException e) {
            iniciar.countDown();
            for (Thread pedido : pedidos) pedido.interrupt();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A simulacao concorrente foi interrompida.", e);
        }

        System.out.println("Resultado do primeiro cliente: " + resultados[0]);
        System.out.println("Resultado do segundo cliente: " + resultados[1]);
    }

    public void formalizarContrato() {
        listarReservas();
        String idReserva = lerTexto("ID da reserva: ");
        int meses = lerInteiro("Duracao do aluguer (1 a 6 meses; para venda introduza 0): ");
        Contracto contrato = automozService.formalizarContrato(idReserva, meses);
        System.out.println("Contrato " + contrato.getId() + " formalizado. Total: " + automozService.calcularValorContrato(contrato));
    }

    public void cancelarReserva() {
        automozService.cancelarReserva(lerTexto("ID da reserva a cancelar: "));
        System.out.println("Reserva cancelada e veiculo libertado.");
    }

    public void listarReservas() {
        if (automozService.listarReservas().isEmpty()) { System.out.println("Ainda nao existem reservas."); return; }
        for (Reserva r : automozService.listarReservas())
            System.out.println(r.getId() + " | " + r.getCliente().getNome() + " | " + r.getVeiculo().getMarca() + " " +
                    r.getVeiculo().getModelo() + " | " + r.getEstado().getDescricao() + " | expira " + r.getDataExpiracao());
    }

    public void listarContratos() {
        if (automozService.listarContratos().isEmpty()) { System.out.println("Ainda nao existem contratos."); return; }
        for (Contracto c : automozService.listarContratos())
            System.out.println(c.getId() + " | " + c.getClass().getSimpleName() + " | " + c.getCliente().getNome() +
                    " | veiculo #" + c.getVeiculo().getId() + " | total: " + automozService.calcularValorContrato(c));
    }

    private TipoVeiculo lerTipo() {
        while (true) {
            String tipo = lerTexto("Tipo (1-Venda, 2-Aluguer): ");
            if ("1".equals(tipo)) return TipoVeiculo.VENDA;
            if ("2".equals(tipo)) return TipoVeiculo.ALUGUER;
            System.out.println("Escolha 1 ou 2.");
        }
    }

    private TipoVeiculo lerTipoCliente() {
        while (true) {
            String tipo = lerTexto("Intencao do cliente (1-Aluguer, 2-Venda): ");
            if ("1".equals(tipo)) return TipoVeiculo.ALUGUER;
            if ("2".equals(tipo)) return TipoVeiculo.VENDA;
            System.out.println("Escolha 1 para aluguer ou 2 para venda.");
        }
    }

    private String lerTexto(String prompt) {
        System.out.print(prompt);
        String valor = input.nextLine().trim();
        if (valor.isEmpty()) throw new IllegalArgumentException("O campo nao pode ficar vazio.");
        return valor;
    }

    private int lerInteiro(String prompt) {
        try { return Integer.parseInt(lerTexto(prompt)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Introduza um numero inteiro valido."); }
    }

    private String lerContacto() {
        while (true) {
            System.out.print("Contacto (9 digitos; ou CANCELAR): ");
            String contacto = input.nextLine().trim();
            if ("CANCELAR".equalsIgnoreCase(contacto)) return null;
            if (contacto.matches("[0-9]{9}")) return contacto;
            System.out.println("Contacto invalido. Digite exatamente 9 digitos numericos ou CANCELAR.");
        }
    }

    private double lerDouble(String prompt) {
        try { return Double.parseDouble(lerTexto(prompt).replace(',', '.')); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Introduza um valor numerico valido."); }
    }
}
