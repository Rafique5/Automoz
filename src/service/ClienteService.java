package service;

import model.Cliente;

public class ClienteService {
    private final PessoaService pessoaService = new PessoaService();

    public Cliente criar(String id, String nome, String email, String contacto, String cartaConducao) {
        pessoaService.validarDados(id, nome, email, contacto);
        String carta = cartaConducao == null ? "" : cartaConducao.trim();
        return new Cliente(id, nome.trim(), email.trim(), contacto, carta);
    }

    public void validarEmail(String email) { pessoaService.validarEmail(email); }

    public void validar(Cliente cliente) {
        pessoaService.validar(cliente);
        if (cliente.getCartaConducao() == null) throw new IllegalArgumentException("Dados de carta de conducao invalidos.");
    }
}
