package service;

import model.Cliente;
import model.Contracto;
import model.Reserva;
import model.TipoVeiculo;
import model.Veiculo;
import model.PeriodoAluguer;

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
        repository.carregar(veiculos, clientes, reservas, contractos);
    }

    public Veiculo registarVeiculo(String marca, String modelo, double preco, TipoVeiculo tipo) {
        proximoVeiculo = repository.proximoVeiculo();
        Veiculo veiculo = veiculoService.criar(proximoVeiculo, marca, modelo, preco, tipo);
        repository.guardar(veiculo);
        veiculos.put(proximoVeiculo++, veiculo);
        return veiculo;
    }

    public Cliente registarCliente(String nome, String email, String contacto, String cartaConducao, TipoVeiculo intencao) {
        String id;
        if (intencao == TipoVeiculo.ALUGUER) id = "CALG300" + String.format("%03d", repository.proximoId("clientes", "id", "CALG300"));
        else if (intencao == TipoVeiculo.VENDA) id = "CVD780" + String.format("%03d", repository.proximoId("clientes", "id", "CVD780"));
        else throw new IllegalArgumentException("Intencao do cliente invalida.");
        Cliente cliente = clienteService.criar(id, nome, email, contacto, cartaConducao, intencao);
        repository.guardar(cliente);
        clientes.put(id, cliente);
        return cliente;
    }

    /** Serializa pedidos concorrentes para que um veiculo só possa ser reservado uma vez. */
    public synchronized Reserva criarReserva(String idCliente, int idVeiculo) {
        Cliente cliente = obterCliente(idCliente);
        Veiculo veiculo = obterVeiculo(idVeiculo);
        String id = "R" + repository.proximoId("reservas", "id", "R");
        Reserva reserva = reservaService.criar(id, cliente, veiculo, LocalDateTime.now());
        repository.guardar(reserva);
        repository.atualizar(veiculo);
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
        Contracto contrato = contractoService.formalizar("C" + repository.proximoId("contratos", "id", "C"),
                reserva, periodo, dataInicio);
        repository.guardar(contrato, reserva);
        repository.atualizar(reserva);
        repository.atualizar(contrato.getVeiculo());
        contractos.add(contrato);
        return contrato;
    }

    public void cancelarReserva(String idReserva) {
        Reserva reserva = obterReserva(idReserva);
        reservaService.cancelar(reserva);
        repository.atualizar(reserva);
        repository.atualizar(reserva.getVeiculo());
    }

    public void atualizarReservasExpiradas() {
        for (Reserva reserva : reservas.values()) {
            if (reservaService.expirarSeNecessario(reserva)) {
                repository.atualizar(reserva);
                repository.atualizar(reserva.getVeiculo());
            }
        }
    }

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
