package service;

import model.EstadoVeiculo;
import model.TipoVeiculo;
import model.Veiculo;

public class VeiculoService {
    public Veiculo criar(int id, String marca, String modelo, double preco, TipoVeiculo tipo) {
        if (id <= 0 || marca == null || marca.trim().isEmpty() || modelo == null || modelo.trim().isEmpty())
            throw new IllegalArgumentException("ID, marca e modelo sao obrigatorios.");
        if (preco <= 0 || Double.isNaN(preco) || Double.isInfinite(preco))
            throw new IllegalArgumentException("O preco deve ser maior que zero.");
        if (tipo == null) throw new IllegalArgumentException("O tipo do veiculo e obrigatorio.");
        return new Veiculo(id, marca.trim(), modelo.trim(), preco, tipo, EstadoVeiculo.DISPONIVEL);
    }

    public void reservar(Veiculo veiculo) {
        validar(veiculo);
        exigirEstado(veiculo, EstadoVeiculo.DISPONIVEL, "Veiculo indisponivel para reserva.");
        veiculo.setEstado(EstadoVeiculo.RESERVADO);
    }

    public void libertar(Veiculo veiculo) {
        validar(veiculo);
        if (veiculo.getEstado() == EstadoVeiculo.RESERVADO || veiculo.getEstado() == EstadoVeiculo.ALUGADO)
            veiculo.setEstado(EstadoVeiculo.DISPONIVEL);
    }

    public void marcarComoVendido(Veiculo veiculo) {
        validar(veiculo);
        if (veiculo.getTipo() != TipoVeiculo.VENDA)
            throw new IllegalArgumentException("Veiculo de aluguer nao pode ser vendido.");
        exigirDisponivelOuReservado(veiculo);
        veiculo.setEstado(EstadoVeiculo.VENDIDO);
    }

    public void marcarComoAlugado(Veiculo veiculo) {
        validar(veiculo);
        if (veiculo.getTipo() != TipoVeiculo.ALUGUER)
            throw new IllegalArgumentException("Veiculo de venda nao pode ser alugado.");
        exigirDisponivelOuReservado(veiculo);
        veiculo.setEstado(EstadoVeiculo.ALUGADO);
    }

    public void alterarPreco(Veiculo veiculo, double preco) {
        validar(veiculo);
        if (preco <= 0 || Double.isNaN(preco) || Double.isInfinite(preco))
            throw new IllegalArgumentException("O preco deve ser maior que zero.");
        veiculo.setPreco(preco);
    }

    public void alterarTipo(Veiculo veiculo, TipoVeiculo tipo) {
        validar(veiculo);
        if (tipo == null) throw new IllegalArgumentException("Tipo obrigatorio.");
        if (veiculo.getEstado() != EstadoVeiculo.DISPONIVEL)
            throw new IllegalStateException("So e possivel alterar o tipo de um veiculo disponivel.");
        veiculo.setTipo(tipo);
    }

    public void validar(Veiculo veiculo) {
        if (veiculo == null) throw new IllegalArgumentException("Veiculo obrigatorio.");
        if (veiculo.getId() <= 0 || vazio(veiculo.getMarca()) || vazio(veiculo.getModelo()))
            throw new IllegalArgumentException("ID, marca e modelo sao obrigatorios.");
        if (veiculo.getPreco() <= 0 || Double.isNaN(veiculo.getPreco()) || Double.isInfinite(veiculo.getPreco()))
            throw new IllegalArgumentException("O preco deve ser maior que zero.");
        if (veiculo.getTipo() == null || veiculo.getEstado() == null)
            throw new IllegalArgumentException("Tipo e estado do veiculo sao obrigatorios.");
    }

    private void exigirDisponivelOuReservado(Veiculo veiculo) {
        if (veiculo.getEstado() != EstadoVeiculo.DISPONIVEL && veiculo.getEstado() != EstadoVeiculo.RESERVADO)
            throw new IllegalStateException("O veiculo nao esta disponivel.");
    }

    private void exigirEstado(Veiculo veiculo, EstadoVeiculo estado, String mensagem) {
        if (veiculo.getEstado() != estado) throw new IllegalStateException(mensagem);
    }

    private boolean vazio(String valor) { return valor == null || valor.trim().isEmpty(); }
}
