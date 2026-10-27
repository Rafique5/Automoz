package service;

import model.Cliente;
import model.TipoVeiculo;

public class ClienteService {
    private final PessoaService pessoaService = new PessoaService();

    public Cliente criar(String id, String nome, String email, String contacto, String cartaConducao, TipoVeiculo intencao) {
        pessoaService.validarDados(id, nome, email, contacto);
        if (intencao == null) throw new IllegalArgumentException("A intencao de compra ou aluguer e obrigatoria.");
        if (intencao == TipoVeiculo.ALUGUER && (cartaConducao == null || cartaConducao.trim().isEmpty()))
            throw new IllegalArgumentException("A carta de conducao e obrigatoria.");
        String carta = cartaConducao == null ? "" : cartaConducao.trim();
        return new Cliente(id, nome.trim(), email.trim(), contacto, carta, intencao);
    }

    public void validar(Cliente cliente) {
        pessoaService.validar(cliente);
        if (cliente.getIntencao() == null) throw new IllegalArgumentException("A intencao do cliente e obrigatoria.");
        if (cliente.getIntencao() == TipoVeiculo.ALUGUER &&
                (cliente.getCartaConducao() == null || cliente.getCartaConducao().trim().isEmpty()))
            throw new IllegalArgumentException("A carta de conducao e obrigatoria.");
    }
}
