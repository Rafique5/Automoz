package app;

import service.AutomozService;
import service.MenuService;

import java.util.Scanner;

/** Interface de consola; as regras de negocio permanecem nos servicos. */
public class AutomozMenu {
    private final Scanner input = new Scanner(System.in);
    private final AutomozService service = new AutomozService();
    private final MenuService menuService = new MenuService(input, service);

    public void executar() {
        boolean continuar = true;
        while (continuar) {
            service.atualizarReservasExpiradas();
            mostrarMenu();
            try {
                switch (lerInteiro("Opcao: ")) {
                    case 1: menuService.registarVeiculo(); break;
                    case 2: menuService.registarCliente(); break;
                    case 3: menuService.listarVeiculos(); break;
                    case 4: menuService.listarClientes(); break;
                    case 5: menuService.criarReserva(); break;
                    case 6: menuService.formalizarContrato(); break;
                    case 7: menuService.cancelarReserva(); break;
                    case 8: menuService.listarReservas(); break;
                    case 9: menuService.listarContratos(); break;
                    case 0: continuar = false; break;
                    default: System.out.println("Opcao invalida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Operacao recusada: " + e.getMessage());
            }
        }
        input.close();
    }

    private void mostrarMenu() {
        System.out.println("\n=== AUTOMOZ ===");
        System.out.println("1. Registar veiculo\n2. Registar cliente\n3. Listar veiculos\n4. Listar clientes\n5. Criar reserva");
        System.out.println("6. Confirmar reserva e formalizar contrato\n7. Cancelar reserva\n8. Listar reservas\n9. Listar contratos\n0. Sair");
    }

    private int lerInteiro(String prompt) {
        try { return Integer.parseInt(input.nextLine().trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Introduza um numero inteiro valido."); }
    }
}
