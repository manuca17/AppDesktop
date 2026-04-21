package com.example.appdesktop.models;

public class Utilizador {

    private static volatile Utilizador currentUser;

    private Integer id;
    private String nomeEmpresa;
    private String nif;
    private String email;
    private String perfil;
    private String password;
    private String telefone;
    private String moradaFaturacao;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getMoradaFaturacao() {
        return moradaFaturacao;
    }

    public void setMoradaFaturacao(String moradaFaturacao) {
        this.moradaFaturacao = moradaFaturacao;
    }

    public static Utilizador getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Utilizador currentUser) {
        Utilizador.currentUser = currentUser;
    }

    public static void clearCurrentUser() {
        currentUser = null;
    }
}
