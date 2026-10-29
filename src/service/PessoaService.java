package service;

import model.Pessoa;

public class PessoaService {
    public void validarDados(String id, String nome, String email, String contacto) {
        if (id == null || id.trim().isEmpty() || nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("ID e nome da pessoa sao obrigatorios.");
        validarEmail(email);
        if (contacto == null || !contacto.matches("[0-9]{9}"))
            throw new IllegalArgumentException("Contacto deve conter exatamente 9 digitos numericos.");
    }

    public void validarEmail(String email) {
        if (email == null) throw new IllegalArgumentException("Email invalido.");
        String valor = email.trim();
        String regex = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$";
        if (valor.length() > 254 || !valor.matches(regex) || valor.contains("..") || valor.startsWith(".") ||
                valor.substring(0, valor.indexOf('@')).endsWith("."))
            throw new IllegalArgumentException("Email invalido.");
    }

    public void validar(Pessoa pessoa) {
        if (pessoa == null) throw new IllegalArgumentException("Pessoa obrigatoria.");
        validarDados(pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), pessoa.getContacto());
    }
}
