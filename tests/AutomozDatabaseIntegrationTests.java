package tests;

import model.*;
import service.AutomozService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Testes de integração: requerem o MySQL local e o esquema automoz.sql atualizado. */
public final class AutomozDatabaseIntegrationTests {
    public static void main(String[] args) throws Exception {
        String token=UUID.randomUUID().toString().replace("-","");
        String emailPrefix="automoz-test-"+token;
        List<Integer> vehicleIds=new ArrayList<Integer>();
        AutomozService seed=new AutomozService();
        try {
            String admin=System.getenv().getOrDefault("AUTOMOZ_ADMIN_USER","admin");
            String password=System.getenv().getOrDefault("AUTOMOZ_ADMIN_PASSWORD","admin123");
            check(seed.autenticarFuncionario(admin,password),"autenticação de funcionário inicial");
            String[] clients=new String[3];
            for(int i=0;i<3;i++)clients[i]=seed.registarCliente("Teste "+i,emailPrefix+"-"+i+"@example.test","84123"+String.format("%04d",i),"CARTA-"+i).getId();
            Cliente sessao=seed.autenticarCliente(emailPrefix+"-0@example.test","841230000");
            check(sessao!=null&&sessao.getId().equals(clients[0]),"cliente inicia sessao com email e telefone");
            check(seed.autenticarCliente(emailPrefix+"-0@example.test","841239999")==null,"telefone incorreto recusa sessao");
            expect(IllegalArgumentException.class,()->seed.registarCliente("Duplicado",emailPrefix+"-0@example.test","841239999","CARTA"));

            Veiculo rental=seed.registarVeiculo("Teste","Aluguer",900.0,TipoVeiculo.ALUGUER);vehicleIds.add(rental.getId());
            LocalDateTime start=LocalDateTime.now().plusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0);
            AutomozService first=new AutomozService(),second=new AutomozService();
            AtomicInteger successes=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
            CountDownLatch ready=new CountDownLatch(3),go=new CountDownLatch(1);
            final CountDownLatch rentalReady=ready,rentalGo=go;
            Thread[] renters=new Thread[3];
            for(int i=0;i<3;i++){
                final int index=i;final AutomozService service=i%2==0?first:second;
                renters[i]=new Thread(()->{rentalReady.countDown();try{rentalGo.await();service.criarReserva(clients[index],rental.getId(),start,PeriodoAluguer.SEMANAL);successes.incrementAndGet();}catch(IllegalStateException expected){if(expected.getMessage()==null||!expected.getMessage().contains("intervalo"))failure.compareAndSet(null,expected);}catch(Throwable e){failure.compareAndSet(null,e);}},"rental-db-test-"+i);
                renters[i].start();
            }
            ready.await();go.countDown();for(Thread t:renters)t.join();
            if(failure.get()!=null)throw new AssertionError("erro inesperado na reserva concorrente de aluguer",failure.get());
            check(successes.get()==1,"concorrência: só uma reserva de aluguer pode vencer no mesmo período");

            Reserva rentalReservation=findReservation(first,second,rental.getId(),start);
            AutomozService winner=first.listarReservas().contains(rentalReservation)?first:second;
            Contracto rent=winner.formalizarContrato(rentalReservation.getId(),null,null);
            check(rent instanceof ContractoAluguer,"formalização do contrato de aluguer");
            ContractoAluguer rentalContract=(ContractoAluguer)rent;
            AutomozService reloaded=new AutomozService();ContractoAluguer persisted=null;
            for(Contracto c:reloaded.listarContratos())if(c.getId().equals(rent.getId()))persisted=(ContractoAluguer)c;
            check(persisted!=null&&persisted.getPeriodo()==PeriodoAluguer.SEMANAL&&persisted.getDataInicio().equals(start),"período e datas do aluguer persistidos no MySQL");
            check(reloaded.calcularValorContrato(persisted)==210.00,"mensalidade semanal persistida/cálculo do contrato");
            LocalDateTime nextStart=rentalContract.getDataFim().plusDays(2);
            Reserva later=winner.criarReserva(clients[1],rental.getId(),nextStart,PeriodoAluguer.UM_DIA);
            check(later.getDataInicioUtilizacao().equals(nextStart),"reserva futura não conflituosa após aluguer");

            Veiculo sale=seed.registarVeiculo("Teste","Venda",15000.0,TipoVeiculo.VENDA);vehicleIds.add(sale.getId());
            AutomozService salesA=new AutomozService(),salesB=new AutomozService();successes.set(0);failure.set(null);ready=new CountDownLatch(3);go=new CountDownLatch(1);
            final CountDownLatch saleReady=ready,saleGo=go;Thread[] buyers=new Thread[3];
            for(int i=0;i<3;i++){
                final int index=i;final AutomozService service=i%2==0?salesA:salesB;
                buyers[i]=new Thread(()->{saleReady.countDown();try{saleGo.await();service.criarReserva(clients[index],sale.getId(),null,null);successes.incrementAndGet();}catch(IllegalStateException expected){if(expected.getMessage()==null||!expected.getMessage().contains("reservado"))failure.compareAndSet(null,expected);}catch(Throwable e){failure.compareAndSet(null,e);}},"sale-db-test-"+i);
                buyers[i].start();
            }
            ready.await();go.countDown();for(Thread t:buyers)t.join();
            if(failure.get()!=null)throw new AssertionError("erro inesperado na reserva concorrente de venda",failure.get());
            check(successes.get()==1,"concorrência: só uma reserva de venda pode vencer");

            Veiculo currentRental=seed.registarVeiculo("Teste","Devolucao",1200.0,TipoVeiculo.ALUGUER);vehicleIds.add(currentRental.getId());
            LocalDateTime today=LocalDateTime.now().toLocalDate().atStartOfDay();
            Reserva currentBooking=seed.criarReserva(clients[2],currentRental.getId(),today,PeriodoAluguer.UM_DIA);
            seed.formalizarContrato(currentBooking.getId(),null,null);
            check(currentRental.getEstado()==EstadoVeiculo.ALUGADO,"veiculo passa a ALUGADO no início do período");
            seed.devolverVeiculo(currentRental.getId());
            check(currentRental.getEstado()==EstadoVeiculo.DISPONIVEL,"devolução liberta o veículo sem reserva futura");
            System.out.println("Testes de integração aprovados: autenticação, duplicados e reservas concorrentes/futuras.");
        } finally {limpar(emailPrefix,vehicleIds);}
    }

    private static Reserva findReservation(AutomozService a,AutomozService b,int vehicle,LocalDateTime start){
        for(AutomozService s:new AutomozService[]{a,b})for(Reserva r:s.listarReservas())if(r.getVeiculo().getId()==vehicle&&start.equals(r.getDataInicioUtilizacao()))return r;
        throw new AssertionError("reserva criada não encontrada nas instâncias de serviço");
    }
    private static void limpar(String prefix,List<Integer> vehicleIds)throws Exception {
        String url=System.getenv().getOrDefault("AUTOMOZ_DB_URL","jdbc:mysql://localhost:3306/automoz_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        String user=System.getenv().getOrDefault("AUTOMOZ_DB_USER","root"),password=System.getenv().getOrDefault("AUTOMOZ_DB_PASSWORD","");
        try(Connection c=DriverManager.getConnection(url,user,password)){
            for(int id:vehicleIds){
                try(PreparedStatement p=c.prepareStatement("DELETE ct FROM contratos ct JOIN clientes cl ON cl.id=ct.cliente_id WHERE cl.email LIKE ? AND ct.veiculo_id=?")){p.setString(1,prefix+"%");p.setInt(2,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE r FROM reservas r JOIN clientes cl ON cl.id=r.cliente_id WHERE cl.email LIKE ? AND r.veiculo_id=?")){p.setString(1,prefix+"%");p.setInt(2,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM veiculos WHERE id=?")){p.setInt(1,id);p.executeUpdate();}
            }
            try(PreparedStatement p=c.prepareStatement("DELETE FROM clientes WHERE email LIKE ?")){p.setString(1,prefix+"%");p.executeUpdate();}
        }
    }
    private static void check(boolean result,String name){if(!result)throw new AssertionError(name);}
    private static void expect(Class<? extends Throwable> type,Runnable action){try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError("unexpected exception",e);}throw new AssertionError("expected "+type.getSimpleName());}
}
