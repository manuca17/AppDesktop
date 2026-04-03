package com.example.appdesktop.models;

import java.time.Instant;

public class ProjetoPersonalizado {

    private Integer id;
    private Utilizador idUtilizador;
    private Integer idArtesa;
    private String tituloProjeto;
    private String briefing;
    private Instant dataCriacao;
    private String estadoAtual;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilizador getIdUtilizador() {
        return idUtilizador;
    }

    public void setIdUtilizador(Utilizador idUtilizador) {
        this.idUtilizador = idUtilizador;
    }

    public Integer getIdArtesa() {
        return idArtesa;
    }

    public void setIdArtesa(Integer idArtesa) {
        this.idArtesa = idArtesa;
    }

    public String getTituloProjeto() {
        return tituloProjeto;
    }

    public void setTituloProjeto(String tituloProjeto) {
        this.tituloProjeto = tituloProjeto;
    }

    public String getBriefing() {
        return briefing;
    }

    public void setBriefing(String briefing) {
        this.briefing = briefing;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Instant dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public String getEstadoAtual() {
        return estadoAtual;
    }

    public void setEstadoAtual(String estadoAtual) {
        this.estadoAtual = estadoAtual;
    }
}
