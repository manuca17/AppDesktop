package com.example.appdesktop.models;

import java.time.Instant;

public class MensagemChat {

    private Integer id;
    private ProjetoPersonalizado idProjeto;
    private Utilizador idRemetenteUtilizador;
    private Artesa idRemetenteArtesa;
    private String conteudo;
    private String urlFoto;
    private Instant dataEnvio;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ProjetoPersonalizado getIdProjeto() {
        return idProjeto;
    }

    public void setIdProjeto(ProjetoPersonalizado idProjeto) {
        this.idProjeto = idProjeto;
    }

    public Utilizador getIdRemetenteUtilizador() {
        return idRemetenteUtilizador;
    }

    public void setIdRemetenteUtilizador(Utilizador idRemetenteUtilizador) {
        this.idRemetenteUtilizador = idRemetenteUtilizador;
    }

    public Artesa getIdRemetenteArtesa() {
        return idRemetenteArtesa;
    }

    public void setIdRemetenteArtesa(Artesa idRemetenteArtesa) {
        this.idRemetenteArtesa = idRemetenteArtesa;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getUrlFoto() {
        return urlFoto;
    }

    public void setUrlFoto(String urlFoto) {
        this.urlFoto = urlFoto;
    }

    public Instant getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(Instant dataEnvio) {
        this.dataEnvio = dataEnvio;
    }
}
