package tests;

import model.*;
import service.ClienteService;
import service.ContractoAluguerService;
import service.ReservaService;
import service.VeiculoService;
import java.time.LocalDateTime;

/** Testes de domínio executáveis sem servidor MySQL: java tests.AutomozDomainTests. */
public final class AutomozDomainTests {
    private static int passed;

    public static void main(String[] args) {
        emailValidoEInvalido();
        clienteSemIntencaoEValido();
        intervaloFuturoPorPeriodo();
        aluguerExigeCarta();
        mensalidadesCalculadasComArredondamento();
        intervalosAdjacentesNaoSeSobrepoem();
        System.out.println("Testes de dominio aprovados: " + passed);
    }

    private static void emailValidoEInvalido() {
        ClienteService service=new ClienteService();
        service.validarEmail("ana.matos@example.co.mz");ok();
        expect(IllegalArgumentException.class,()->service.validarEmail("ana@@example.com"));
        expect(IllegalArgumentException.class,()->service.validarEmail("ana..matos@example.com"));
        expect(IllegalArgumentException.class,()->service.validarEmail("ana@example"));
    }

    private static void clienteSemIntencaoEValido() {
        Cliente cliente=new ClienteService().criar("CL000001","Ana","ana@example.com","841234567","");
        check(cliente.getId().equals("CL000001"),"ID do cliente deve ser preservado");
    }

    private static void intervaloFuturoPorPeriodo() {
        Cliente cliente=clienteComCarta();Veiculo veiculo=veiculoAluguer();
        LocalDateTime inicio=LocalDateTime.now().plusDays(10).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Reserva reserva=new ReservaService().criar("R1",cliente,veiculo,LocalDateTime.now(),inicio,PeriodoAluguer.SEMANAL);
        check(reserva.getDataFimUtilizacao().equals(inicio.plusWeeks(1)),"Fim de reserva semanal incorreto");
        check(reserva.getPeriodo()==PeriodoAluguer.SEMANAL,"Periodo nao guardado na reserva");
    }

    private static void aluguerExigeCarta() {
        Cliente semCarta=new ClienteService().criar("CL000002","Bruno","bruno@example.com","841234568","");
        expect(IllegalStateException.class,()->new ReservaService().criar("R2",semCarta,veiculoAluguer(),
                LocalDateTime.now(),LocalDateTime.now().plusDays(1),PeriodoAluguer.UM_DIA));
    }

    private static void mensalidadesCalculadasComArredondamento() {
        Cliente cliente=clienteComCarta();Veiculo veiculo=veiculoAluguer();
        ContractoAluguerService service=new ContractoAluguerService();LocalDateTime inicio=LocalDateTime.now().plusDays(1);
        double diaria=service.calcularValorTotal(service.criar("C1",cliente,veiculo,PeriodoAluguer.UM_DIA,inicio,1000.0,LocalDateTime.now()));
        double duas=service.calcularValorTotal(service.criar("C2",cliente,veiculo,PeriodoAluguer.DOIS_DIAS,inicio,1000.0,LocalDateTime.now()));
        double semanal=service.calcularValorTotal(service.criar("C3",cliente,veiculo,PeriodoAluguer.SEMANAL,inicio,1000.0,LocalDateTime.now()));
        double mensal=service.calcularValorTotal(service.criar("C4",cliente,veiculo,PeriodoAluguer.MENSAL,inicio,1000.0,LocalDateTime.now()));
        check(diaria==33.33,"Valor diário deveria arredondar para 33.33");
        check(duas==66.67,"Valor de dois dias deveria arredondar para 66.67");
        check(semanal==233.33,"Valor semanal deveria arredondar para 233.33");
        check(mensal==1000.00,"Valor mensal incorreto");
    }

    private static void intervalosAdjacentesNaoSeSobrepoem() {
        LocalDateTime a=LocalDateTime.of(2027,1,1,0,0),b=a.plusDays(2),c=b,d=c.plusDays(1);
        check(!sobrepoe(a,b,c,d),"Intervalos encostados devem ser permitidos");
        check(sobrepoe(a,b,b.minusNanos(1),d),"Intervalos com interseção devem conflitar");
    }

    private static boolean sobrepoe(LocalDateTime a1,LocalDateTime a2,LocalDateTime b1,LocalDateTime b2){return a1.isBefore(b2)&&a2.isAfter(b1);}
    private static Cliente clienteComCarta(){return new ClienteService().criar("CL000003","Carla","carla@example.com","841234569","CARTA-123");}
    private static Veiculo veiculoAluguer(){return new VeiculoService().criar(1,"Toyota","Corolla",1000.0,TipoVeiculo.ALUGUER);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);ok();}
    private static void expect(Class<? extends Throwable> expected,Runnable action){try{action.run();}catch(Throwable e){if(expected.isInstance(e)){ok();return;}throw new AssertionError("Excecao inesperada: "+e,e);}throw new AssertionError("Esperava "+expected.getSimpleName());}
    private static void ok(){passed++;}
}
