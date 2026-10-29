package service;

import model.Cliente;
import model.Contracto;
import model.Reserva;
import model.TipoVeiculo;
import model.Veiculo;
import model.PeriodoAluguer;
import model.EstadoVeiculo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Camada de servico: centraliza regras e casos de uso da aplicacao. */
public class AutomozService {
    private final Map<Integer, Veiculo> veiculos = new LinkedHashMap<Integer, Veiculo>();
    private final Map<String, Cliente> clientes = new LinkedHashMap<String, Cliente>();
    private final Map<String, Reserva> reservas = new LinkedHashMap<String, Reserva>();
    private final List<Contracto> contractos = new ArrayList<Contracto>();
    private int proximoVeiculo = 1;
    private final ClienteService clienteService = new ClienteService();
    private final VeiculoService veiculoService = new VeiculoService();
    private final ReservaService reservaService = new ReservaService();
    private final ContractoService contractoService = new ContractoService();
    private final AutomozRepository repository = new AutomozRepository();

    public AutomozService() {
        repository.criarFuncionarioInicial(System.getenv().getOrDefault("AUTOMOZ_ADMIN_USER", "admin"),
                System.getenv().getOrDefault("AUTOMOZ_ADMIN_PASSWORD", "admin123"));
        repository.prepararSequencias();
        repository.carregar(veiculos, clientes, reservas, contractos);
    }

    public Veiculo registarVeiculo(String marca, String modelo, double preco, TipoVeiculo tipo) {
        proximoVeiculo = repository.proximoVeiculo();
        Veiculo veiculo = veiculoService.criar(proximoVeiculo, marca, modelo, preco, tipo);
        repository.guardar(veiculo);
        veiculos.put(proximoVeiculo++, veiculo);
        return veiculo;
    }

    public Cliente registarCliente(String nome, String email, String contacto, String cartaConducao) {
        clienteService.validarEmail(email);
        if (repository.emailRegistado(email)) throw new IllegalArgumentException("Ja existe um cliente com este email.");
        String id = "CL" + String.format("%06d", repository.proximoId("clientes", "id", "CL"));
        Cliente cliente = clienteService.criar(id, nome, email, contacto, cartaConducao);
        repository.guardar(cliente);
        clientes.put(id, cliente);
        return cliente;
    }

    public void validarEmailDisponivel(String email) {
        clienteService.validarEmail(email);
        if(repository.emailRegistado(email))throw new IllegalArgumentException("Ja existe um cliente com este email.");
    }

    public void exigirCliente(String id) { obterCliente(id); }

    public Cliente autenticarCliente(String email,String contacto) {
        if(email==null||contacto==null)return null;
        String emailNormalizado=email.trim();
        clienteService.validarEmail(emailNormalizado);
        if(!contacto.trim().matches("[0-9]{9}"))throw new IllegalArgumentException("Contacto deve conter exatamente 9 digitos numericos.");
        for(Cliente cliente:clientes.values())
            if(cliente.getEmail().equalsIgnoreCase(emailNormalizado)&&cliente.getContacto().equals(contacto.trim()))return cliente;
        return null;
    }

    public boolean autenticarFuncionario(String usuario,String senha){return repository.autenticarFuncionario(usuario,senha);}
    public void alterarSenhaFuncionario(String usuario,String atual,String nova){
        if(nova==null||nova.length()<8)throw new IllegalArgumentException("A nova palavra-passe deve ter pelo menos 8 caracteres.");
        repository.alterarSenhaFuncionario(usuario,atual,nova);
    }

    /** Serializa pedidos concorrentes para que um veiculo só possa ser reservado uma vez. */
    public synchronized Reserva criarReserva(String idCliente, int idVeiculo, LocalDateTime inicioUtilizacao, PeriodoAluguer periodo) {
        Cliente cliente = obterCliente(idCliente);
        Veiculo veiculo = obterVeiculo(idVeiculo);
        String id = "R" + repository.proximoId("reservas", "id", "R");
        Reserva reserva = reservaService.criar(id, cliente, veiculo, LocalDateTime.now(), inicioUtilizacao, periodo);
        repository.criarReserva(reserva);
        reservas.put(id, reserva);
        return reserva;
    }

    /** Confirma a reserva e cria o contrato correspondente ao tipo de veiculo. */
    /** Formaliza o contrato num worker thread e devolve o resultado ao chamador. */
    public Contracto formalizarContrato(final String idReserva, final PeriodoAluguer periodo,
                                        final LocalDateTime dataInicio) {
        final AtomicReference<Contracto> resultado = new AtomicReference<Contracto>();
        final AtomicReference<RuntimeException> erro = new AtomicReference<RuntimeException>();
        Thread threadAluguer = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    resultado.set(formalizarContratoComSeguranca(idReserva, periodo, dataInicio));
                } catch (RuntimeException e) {
                    erro.set(e);
                }
            }
        }, "formalizacao-aluguer");
        threadAluguer.start();
        try {
            threadAluguer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A formalizacao do aluguer foi interrompida.", e);
        }
        if (erro.get() != null) throw erro.get();
        return resultado.get();
    }

    /** A secção crítica protege o estado partilhado quando vários workers formalizam contratos. */
    private synchronized Contracto formalizarContratoComSeguranca(String idReserva, PeriodoAluguer periodo,
                                                                  LocalDateTime dataInicio) {
        Reserva reserva = obterReserva(idReserva);
        Contracto contrato = contractoService.preparar("C" + repository.proximoId("contratos", "id", "C"),
                reserva, periodo == null ? reserva.getPeriodo() : periodo,
                dataInicio == null ? reserva.getDataInicioUtilizacao() : dataInicio);
        repository.guardarContrato(contrato, reserva);
        contractos.add(contrato);
        return contrato;
    }

    public void cancelarReserva(String idReserva) {
        Reserva reserva = obterReserva(idReserva);
        repository.cancelarReserva(reserva);
    }

    public synchronized void atualizarReservasExpiradas() {
        for (Reserva reserva : reservas.values()) {
            if (reservaService.expirarSeNecessario(reserva)) {
                repository.atualizarReserva(reserva);
            }
        }
        Map<Integer,EstadoVeiculo> estados=repository.atualizarEstadosVeiculos();
        for(Map.Entry<Integer,EstadoVeiculo> e:estados.entrySet())if(veiculos.containsKey(e.getKey()))veiculos.get(e.getKey()).setEstado(e.getValue());
    }

    public synchronized void expirarReserva(Reserva reserva) {
        if(reservaService.expirarSeNecessario(reserva))repository.atualizarReserva(reserva);
    }

    public synchronized void devolverVeiculo(int idVeiculo){repository.devolverVeiculo(idVeiculo,contractos,new ArrayList<Reserva>(reservas.values()));}

    public Cliente obterClientePublico(String id){return obterCliente(id);}

    public List<Veiculo> listarVeiculos() { return Collections.unmodifiableList(new ArrayList<Veiculo>(veiculos.values())); }
    public List<Cliente> listarClientes() { return Collections.unmodifiableList(new ArrayList<Cliente>(clientes.values())); }
    public List<Reserva> listarReservas() { return Collections.unmodifiableList(new ArrayList<Reserva>(reservas.values())); }
    public List<Contracto> listarContratos() { return Collections.unmodifiableList(new ArrayList<Contracto>(contractos)); }
    public double calcularValorContrato(Contracto contrato) { return contractoService.calcularValorTotal(contrato); }

    private Cliente obterCliente(String id) {
        Cliente cliente = id == null ? null : clientes.get(id.trim());
        if (cliente == null) throw new IllegalArgumentException("Cliente nao encontrado.");
        return cliente;
    }

    private Veiculo obterVeiculo(int id) {
        Veiculo veiculo = veiculos.get(id);
        if (veiculo == null) throw new IllegalArgumentException("Veiculo nao encontrado.");
        return veiculo;
    }

    private Reserva obterReserva(String id) {
        if (id == null) throw new IllegalArgumentException("Reserva nao encontrada.");
        Reserva reserva = reservas.get(id.trim());
        if (reserva == null) throw new IllegalArgumentException("Reserva nao encontrada.");
        return reserva;
    }
}
