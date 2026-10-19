package service;

import model.Cliente;
import model.Contracto;
import model.Reserva;
import model.TipoVeiculo;
import model.Veiculo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Camada de servico: centraliza regras e casos de uso da aplicacao. */
public class AutomozService {
    private final Map<Integer, Veiculo> veiculos = new LinkedHashMap<Integer, Veiculo>();
    private final Map<String, Cliente> clientes = new LinkedHashMap<String, Cliente>();
    private final Map<String, Reserva> reservas = new LinkedHashMap<String, Reserva>();
    private final List<Contracto> contractos = new ArrayList<Contracto>();
    private int proximoVeiculo = 1;
    private int proximoClienteAluguer = 1;
    private int proximoClienteVenda = 1;
    private int proximaReserva = 1;
    private int proximoContracto = 1;
    private final ClienteService clienteService = new ClienteService();
    private final VeiculoService veiculoService = new VeiculoService();
    private final ReservaService reservaService = new ReservaService();
    private final ContractoService contractoService = new ContractoService();

    public Veiculo registarVeiculo(String marca, String modelo, double preco, TipoVeiculo tipo) {
        Veiculo veiculo = veiculoService.criar(proximoVeiculo, marca, modelo, preco, tipo);
        veiculos.put(proximoVeiculo++, veiculo);
        return veiculo;
    }

    public Cliente registarCliente(String nome, String email, String contacto, String cartaConducao, TipoVeiculo intencao) {
        String id;
        if (intencao == TipoVeiculo.ALUGUER) id = "CALG300" + String.format("%03d", proximoClienteAluguer);
        else if (intencao == TipoVeiculo.VENDA) id = "CVD780" + String.format("%03d", proximoClienteVenda);
        else throw new IllegalArgumentException("Intencao do cliente invalida.");
        Cliente cliente = clienteService.criar(id, nome, email, contacto, cartaConducao, intencao);
        clientes.put(id, cliente);
        if (intencao == TipoVeiculo.ALUGUER) proximoClienteAluguer++;
        else proximoClienteVenda++;
        return cliente;
    }

    public Reserva criarReserva(String idCliente, int idVeiculo) {
        Cliente cliente = obterCliente(idCliente);
        Veiculo veiculo = obterVeiculo(idVeiculo);
        String id = "R" + proximaReserva;
        Reserva reserva = reservaService.criar(id, cliente, veiculo, LocalDateTime.now());
        reservas.put(id, reserva);
        proximaReserva++;
        return reserva;
    }

    /** Confirma a reserva e cria o contrato correspondente ao tipo de veiculo. */
    public Contracto formalizarContrato(String idReserva, int numeroMeses) {
        Reserva reserva = obterReserva(idReserva);
        Contracto contrato = contractoService.formalizar("C" + proximoContracto, reserva, numeroMeses);
        contractos.add(contrato);
        proximoContracto++;
        return contrato;
    }

    public void cancelarReserva(String idReserva) {
        reservaService.cancelar(obterReserva(idReserva));
    }

    public void atualizarReservasExpiradas() {
        for (Reserva reserva : reservas.values()) reservaService.expirarSeNecessario(reserva);
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
