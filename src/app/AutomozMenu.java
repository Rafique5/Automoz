package app;

import model.TipoVeiculo;
import service.AutomozService;
import service.MenuService;
import model.Cliente;

import java.util.Scanner;

/** Porta de acesso: clientes usam funções públicas; o painel de gestão exige autenticação. */
public class AutomozMenu {
    private final Scanner input = new Scanner(System.in);
    private String funcionarioAutenticado;

    public void executar() {
        final AutomozService service;
        try { service = new AutomozService(); }
        catch (IllegalStateException e) { System.out.println("Nao foi possivel iniciar o Automoz: " + e.getMessage()); return; }
        MenuService menu = new MenuService(input, service);
        boolean executar = true;
        while (executar) {
            System.out.println("\n=== AUTOMOZ | ACESSO ===\n1. Cliente\n2. Funcionario\n0. Sair");
            try {
                switch (lerInteiro("Opcao: ")) {
                    case 1: fluxoAcessoCliente(menu, service); break;
                    case 2:
                        if (autenticarFuncionario(service)) executarPainelFuncionario(menu, service);
                        break;
                    case 0: executar = false; break;
                    default: System.out.println("Opcao invalida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Operacao recusada: " + e.getMessage());
            }
        }
        input.close();
    }

    private void fluxoAcessoCliente(MenuService menu,AutomozService service) {
        boolean voltar=false;
        while(!voltar) {
            System.out.println("\n=== AUTOMOZ | CLIENTE ===\n1. Iniciar sessao\n2. Criar conta\n0. Voltar");
            try {
                switch(lerInteiro("Opcao: ")) {
                    case 1:
                        Cliente cliente=autenticarCliente(service);
                        if(cliente!=null)executarPainelCliente(menu,service,cliente);
                        break;
                    case 2:
                        Cliente novo=menu.registarCliente();
                        if(novo!=null)executarPainelCliente(menu,service,novo);
                        break;
                    case 0:voltar=true;break;
                    default:System.out.println("Opcao invalida.");
                }
            } catch(IllegalArgumentException|IllegalStateException e){System.out.println("Operacao recusada: "+e.getMessage());}
        }
    }

    private Cliente autenticarCliente(AutomozService service) {
        System.out.print("Email: ");String email=input.nextLine().trim();
        System.out.print("Numero de telefone: ");String contacto=input.nextLine().trim();
        try {
            Cliente cliente=service.autenticarCliente(email,contacto);
            if(cliente==null)System.out.println("Email e numero de telefone nao correspondem a uma conta. Pode criar uma conta nova.");
            return cliente;
        } catch(IllegalArgumentException e){System.out.println(e.getMessage());return null;}
    }

    private void executarPainelCliente(MenuService menu,AutomozService service,Cliente cliente) {
        boolean voltar=false;
        while(!voltar) {
            service.atualizarReservasExpiradas();
            System.out.println("\n=== AUTOMOZ | CLIENTE: "+cliente.getNome()+" ===\n1. Listar veiculos\n2. Criar reserva\n0. Terminar sessao");
            try {
                switch(lerInteiro("Opcao: ")) {
                    case 1: menu.listarVeiculos();break;
                    case 2: menu.criarReservaCliente(cliente.getId());break;
                    case 0: voltar=true;break;
                    default:System.out.println("Opcao invalida.");
                }
            } catch(IllegalArgumentException|IllegalStateException e){System.out.println("Operacao recusada: "+e.getMessage());}
        }
    }

    private boolean autenticarFuncionario(AutomozService service) {
        while(true) {
            System.out.print("Utilizador (ou VOLTAR): ");String user=input.nextLine().trim();
            if("VOLTAR".equalsIgnoreCase(user))return false;
            System.out.print("Palavra-passe: ");String password=input.nextLine();
            if(service.autenticarFuncionario(user,password)){funcionarioAutenticado=user;System.out.println("Autenticacao concluida.");return true;}
            System.out.println("Credenciais invalidas. Tente novamente ou escreva VOLTAR.");
        }
    }

    private void executarPainelFuncionario(MenuService menu,AutomozService service) {
        boolean voltar=false;
        while(!voltar) {
            service.atualizarReservasExpiradas();
            mostrarMenuFuncionario();
            try {
                switch(lerInteiro("Opcao: ")) {
                    case 1:menu.registarVeiculo();break;
                    case 2:menu.registarCliente();break;
                    case 3:menu.listarVeiculos();break;
                    case 4:menu.listarClientes();break;
                    case 5:menu.criarReserva();break;
                    case 6:menu.formalizarContrato();break;
                    case 7:menu.cancelarReserva();break;
                    case 8:menu.listarReservas();break;
                    case 9:menu.listarContratos();break;
                    case 10:menu.simularReservasConcorrentes();break;
                    case 11:menu.devolverVeiculo();break;
                    case 12:menu.alterarSenhaFuncionario(funcionarioAutenticado);break;
                    case 0:voltar=true;break;
                    default:System.out.println("Opcao invalida.");
                }
            } catch(IllegalArgumentException|IllegalStateException e){System.out.println("Operacao recusada: "+e.getMessage());}
        }
    }

    private void mostrarMenuFuncionario() {
        System.out.println("\n=== AUTOMOZ | PAINEL DO FUNCIONARIO ===");
        System.out.println("1. Registar veiculo\n2. Registar cliente\n3. Listar veiculos\n4. Listar clientes\n5. Criar reserva");
        System.out.println("6. Confirmar reserva e formalizar contrato\n7. Cancelar reserva\n8. Listar reservas\n9. Listar contratos");
        System.out.println("10. Simular tres clientes a reservar em simultaneo\n11. Registar devolucao\n12. Alterar palavra-passe\n0. Voltar ao acesso");
    }

    private int lerInteiro(String prompt) {
        while(true){System.out.print(prompt);String value=input.nextLine().trim();
            try{return Integer.parseInt(value);}catch(NumberFormatException e){System.out.println("Introduza um numero inteiro valido.");}}
    }
}
