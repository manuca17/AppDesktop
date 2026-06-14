package com.example.appdesktop.models;

import java.math.BigDecimal;
import java.util.List;

public class ArtigoCatalogo {

    private Integer id;
    private String nome;
    private BigDecimal precoUnitario;
    private Integer stock;
    private Boolean visivel;
    private String fotoUrl;
    private List<ArtigoFoto> fotos;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getVisivel() {
        return visivel;
    }

    public void setVisivel(Boolean visivel) {
        this.visivel = visivel;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public List<ArtigoFoto> getFotos() {
        return fotos;
    }

    public void setFotos(List<ArtigoFoto> fotos) {
        this.fotos = fotos;
    }
}
