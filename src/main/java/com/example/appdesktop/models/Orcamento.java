package com.example.appdesktop.models;

import java.math.BigDecimal;
import java.time.Instant;

public class Orcamento {

    private Integer id;
    private ProjetoPersonalizado idProjeto;
    private Artesa idArtesa;
    private BigDecimal valorTotalEstimado;
    private Instant dataEnvio;
    private String estado;
    private String observacoes;
    private String tipo;

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

    public Artesa getIdArtesa() {
        return idArtesa;
    }

    public void setIdArtesa(Artesa idArtesa) {
        this.idArtesa = idArtesa;
    }

    public BigDecimal getValorTotalEstimado() {
        return valorTotalEstimado;
    }

    public void setValorTotalEstimado(BigDecimal valorTotalEstimado) {
        this.valorTotalEstimado = valorTotalEstimado;
    }

    public Instant getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(Instant dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
