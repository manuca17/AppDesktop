package com.example.appdesktop.models;

public class FichaTecnica {

    private Integer id;
    private String tipoBarro;
    private String corVidrado;
    private Integer temperaturaCozedura;
    private String tempoSecagem;
    private String observacoes;
    private String fotoDesign;
    private String fotoPrototipo;
    private String refMolde;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipoBarro() {
        return tipoBarro;
    }

    public void setTipoBarro(String tipoBarro) {
        this.tipoBarro = tipoBarro;
    }

    public String getCorVidrado() {
        return corVidrado;
    }

    public void setCorVidrado(String corVidrado) {
        this.corVidrado = corVidrado;
    }

    public Integer getTemperaturaCozedura() {
        return temperaturaCozedura;
    }

    public void setTemperaturaCozedura(Integer temperaturaCozedura) {
        this.temperaturaCozedura = temperaturaCozedura;
    }

    public String getTempoSecagem() {
        return tempoSecagem;
    }

    public void setTempoSecagem(String tempoSecagem) {
        this.tempoSecagem = tempoSecagem;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getFotoDesign() {
        return fotoDesign;
    }

    public void setFotoDesign(String fotoDesign) {
        this.fotoDesign = fotoDesign;
    }

    public String getFotoPrototipo() {
        return fotoPrototipo;
    }

    public void setFotoPrototipo(String fotoPrototipo) {
        this.fotoPrototipo = fotoPrototipo;
    }

    public String getRefMolde() {
        return refMolde;
    }

    public void setRefMolde(String refMolde) {
        this.refMolde = refMolde;
    }
}
