package model;

public class FuncionarioAtendimento extends Pessoa{
    public FuncionarioAtendimento(String id, String nome, String email, String contacto) {
        super(id, nome, email, contacto);
    }

    public FuncionarioAtendimento(int id, String nome, String email, String contacto) {
        this(String.valueOf(id), nome, email, contacto);
    }

    private void validarDocumentos(){}
    private void formalizarContracto(){}
    private void registarPagamento(){}
    private void registarDevolucao(){}
}
