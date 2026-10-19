package model;

public abstract class Pessoa {
    private String id;
    private String nome;
    private String email;
    private String contacto;

    public Pessoa(String id, String nome, String email, String contacto) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.contacto = contacto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }
}
