package service;

import model.Cliente;
import model.Contracto;
import model.Reserva;
import model.TipoVeiculo;
import model.Veiculo;
import model.PeriodoAluguer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
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

    public Cliente registarCliente() {
        String nome = lerTexto("Nome: ");
        String email;
        while (true) {
            email = lerTexto("Email: ");
            try { automozService.validarEmailDisponivel(email); break; }
            catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
        }
        String contacto = lerContacto();
        if (contacto == null) {
            System.out.println("Registo de cliente cancelado.");
            return null;
        }
        System.out.print("Carta de conducao (opcional; obrigatoria para aluguer): ");
        String cartaConducao = input.nextLine().trim();
        try {
            Cliente cliente = automozService.registarCliente(nome, email, contacto, cartaConducao);
            System.out.println("Cliente registado com ID " + cliente.getId());
            return cliente;
        } catch (IllegalArgumentException e) { System.out.println("Registo recusado: " + e.getMessage()); }
        return null;
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
            System.out.println(c.getId() + " " + c.getNome() + " | " + c.getEmail() + " | carta: " + c.getCartaConducao());
    }

    public void criarReserva() {
        String idCliente = lerTexto("ID do cliente: ");
        automozService.exigirCliente(idCliente);
        criarReservaParaCliente(idCliente);
    }

    public void criarReservaCliente(String idCliente) {
        automozService.exigirCliente(idCliente);
        criarReservaParaCliente(idCliente);
    }

    private void criarReservaParaCliente(String idCliente) {
        listarVeiculos();
        int idVeiculo = lerInteiro("ID do veiculo: ");
        Veiculo selecionado = null;
        for(Veiculo v:automozService.listarVeiculos())if(v.getId()==idVeiculo)selecionado=v;
        if(selecionado==null)throw new IllegalArgumentException("Veiculo nao encontrado.");
        PeriodoAluguer periodo=null;LocalDateTime inicio=null;
        if(selecionado.getTipo()==TipoVeiculo.ALUGUER){periodo=lerPeriodoAluguer();inicio=lerDataInicioAluguer();}
        Reserva reserva = automozService.criarReserva(idCliente, idVeiculo, inicio, periodo);
        System.out.println("Reserva " + reserva.getId() + " criada para " +
                (inicio==null?"venda":inicio.toLocalDate()+" ate "+reserva.getDataFimUtilizacao().toLocalDate())+
                "; expira em " + reserva.getDataExpiracao());
    }

    /** Lança três pedidos em paralelo para demonstrar a exclusão mútua na reserva. */
    public void simularReservasConcorrentes() {
        listarVeiculos();
        final int idVeiculo = lerInteiro("ID do veiculo a testar (venda ou aluguer): ");
        Veiculo selecionado=null;
        for(Veiculo v:automozService.listarVeiculos())if(v.getId()==idVeiculo)selecionado=v;
        if(selecionado==null)throw new IllegalArgumentException("Veiculo nao encontrado.");
        final String[] clientes = new String[3];
        for(int i=0;i<clientes.length;i++){
            clientes[i]=lerTexto("ID do cliente "+(i+1)+": ");
            automozService.exigirCliente(clientes[i]);
        }
        final PeriodoAluguer periodo=selecionado.getTipo()==TipoVeiculo.ALUGUER?lerPeriodoAluguer():null;
        final LocalDateTime inicio=periodo==null?null:lerDataInicioAluguer();
        final int veiculo = idVeiculo;
        final String[] resultados = new String[3];
        final CountDownLatch prontos = new CountDownLatch(3);
        final CountDownLatch iniciar = new CountDownLatch(1);
        Thread[] pedidos = new Thread[3];

        for (int i = 0; i < pedidos.length; i++) {
            final int indice = i;
            pedidos[i] = new Thread(new Runnable() {
                @Override public void run() {
                    prontos.countDown();
                    try {
                        iniciar.await();
                        Reserva reserva = automozService.criarReserva(clientes[indice], veiculo, inicio, periodo);
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

        for(int i=0;i<resultados.length;i++)System.out.println("Resultado do cliente "+(i+1)+": "+resultados[i]);
    }

    public void formalizarContrato() {
        listarReservas();
        String idReserva = lerTexto("ID da reserva: ");
        Contracto contrato = automozService.formalizarContrato(idReserva, null, null);
        System.out.println("Contrato " + contrato.getId() + " formalizado. Total: " + automozService.calcularValorContrato(contrato));
    }

    private LocalDateTime lerDataInicioAluguer() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
        while (true) {
            System.out.print("Data de inicio (dd/MM/aaaa; Enter ou HOJE para hoje): ");
            String valor = input.nextLine().trim();
            if (valor.isEmpty() || "HOJE".equalsIgnoreCase(valor)) return LocalDate.now().atStartOfDay();
            try { return LocalDate.parse(valor, formato).atStartOfDay(); }
            catch (DateTimeParseException e) { System.out.println("Data invalida. Use o formato dd/MM/aaaa."); }
        }
    }

    private PeriodoAluguer lerPeriodoAluguer() {
        System.out.println("Periodo: 1. 1 dia | 2. 2 dias | 3. Semanal | 4. Mensal");
        switch (lerInteiro("Escolha o periodo: ")) {
            case 1: return PeriodoAluguer.UM_DIA;
            case 2: return PeriodoAluguer.DOIS_DIAS;
            case 3: return PeriodoAluguer.SEMANAL;
            case 4: return PeriodoAluguer.MENSAL;
            default: throw new IllegalArgumentException("Escolha um periodo entre 1 e 4.");
        }
    }

    public void cancelarReserva() {
        automozService.cancelarReserva(lerTexto("ID da reserva a cancelar: "));
        System.out.println("Reserva cancelada e veiculo libertado.");
    }

    public void devolverVeiculo(){automozService.devolverVeiculo(lerInteiro("ID do veiculo a devolver: "));System.out.println("Devolucao registada; estado do veiculo atualizado.");}

    public void alterarSenhaFuncionario(String utilizador){
        System.out.print("Palavra-passe atual: ");String atual=input.nextLine();
        System.out.print("Nova palavra-passe (minimo 8 caracteres): ");String nova=input.nextLine();
        automozService.alterarSenhaFuncionario(utilizador,atual,nova);
        System.out.println("Palavra-passe alterada.");
    }

    public void listarReservas() {
        if (automozService.listarReservas().isEmpty()) { System.out.println("Ainda nao existem reservas."); return; }
        for (Reserva r : automozService.listarReservas()) {
            String periodo = r.getPeriodo()==null ? "venda" : r.getPeriodo().getDescricao()+" | uso "+
                    r.getDataInicioUtilizacao().toLocalDate()+" a "+r.getDataFimUtilizacao().toLocalDate();
            System.out.println(r.getId() + " | " + r.getCliente().getNome() + " | " + r.getVeiculo().getMarca() + " " +
                    r.getVeiculo().getModelo() + " | " + periodo + " | " + r.getEstado().getDescricao() + " | expira " + r.getDataExpiracao());
        }
    }

    public void listarContratos() {
        if (automozService.listarContratos().isEmpty()) { System.out.println("Ainda nao existem contratos."); return; }
        for (Contracto c : automozService.listarContratos()) {
            String detalheAluguer = "";
            if (c instanceof model.ContractoAluguer) {
                model.ContractoAluguer aluguer = (model.ContractoAluguer) c;
                String periodo = aluguer.getPeriodo() == null
                        ? aluguer.getMesesLegados() + " mes(es) (contrato antigo)"
                        : aluguer.getPeriodo().getDescricao();
                detalheAluguer = " | periodo: " + periodo + " | inicio: " + aluguer.getDataInicio().toLocalDate();
            }
            System.out.println(c.getId() + " | " + c.getClass().getSimpleName() + " | " + c.getCliente().getNome() +
                    " | veiculo #" + c.getVeiculo().getId() + detalheAluguer +
                    " | total: " + automozService.calcularValorContrato(c));
        }
    }

    private TipoVeiculo lerTipo() {
        while (true) {
            String tipo = lerTexto("Tipo (1-Venda, 2-Aluguer): ");
            if ("1".equals(tipo)) return TipoVeiculo.VENDA;
            if ("2".equals(tipo)) return TipoVeiculo.ALUGUER;
            System.out.println("Escolha 1 ou 2.");
        }
    }

    private String lerTexto(String prompt) {
        while(true){System.out.print(prompt);String valor=input.nextLine().trim();if(!valor.isEmpty())return valor;System.out.println("O campo nao pode ficar vazio.");}
    }

    private int lerInteiro(String prompt) {
        while(true){try{return Integer.parseInt(lerTexto(prompt));}catch(NumberFormatException e){System.out.println("Introduza um numero inteiro valido.");}}
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
        while(true){try{return Double.parseDouble(lerTexto(prompt).replace(',','.'));}catch(NumberFormatException e){System.out.println("Introduza um valor numerico valido.");}}
    }
}
