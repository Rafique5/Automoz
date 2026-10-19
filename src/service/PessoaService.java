package service;

import model.Pessoa;

public class PessoaService {
    public void validarDados(String id, String nome, String email, String contacto) {
        if (id == null || id.trim().isEmpty() || nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("ID e nome da pessoa sao obrigatorios.");
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Email invalido.");
        if (contacto == null || !contacto.matches("[0-9]{9}"))
            throw new IllegalArgumentException("Contacto deve conter exatamente 9 digitos numericos.");
    }

    public void validar(Pessoa pessoa) {
        if (pessoa == null) throw new IllegalArgumentException("Pessoa obrigatoria.");
        validarDados(pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), pessoa.getContacto());
    }
}
