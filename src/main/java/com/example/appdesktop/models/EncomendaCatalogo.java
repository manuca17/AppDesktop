package com.example.appdesktop.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EncomendaCatalogo {

    private Integer id;
    private String estado;
    private String estadoPagamento;
    private LocalDate dataEncomenda;
    private Utilizador idUtilizador;
    private ProjetoPersonalizado idProjeto;
    private List<LinhaEncomenda> linhas;
    private BigDecimal total;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEstadoPagamento() { return estadoPagamento; }
    public void setEstadoPagamento(String estadoPagamento) { this.estadoPagamento = estadoPagamento; }

    public LocalDate getDataEncomenda() { return dataEncomenda; }
    public void setDataEncomenda(LocalDate dataEncomenda) { this.dataEncomenda = dataEncomenda; }

    public Utilizador getIdUtilizador() { return idUtilizador; }
    public void setIdUtilizador(Utilizador idUtilizador) { this.idUtilizador = idUtilizador; }

    public ProjetoPersonalizado getIdProjeto() { return idProjeto; }
    public void setIdProjeto(ProjetoPersonalizado idProjeto) { this.idProjeto = idProjeto; }

    public List<LinhaEncomenda> getLinhas() { return linhas; }
    public void setLinhas(List<LinhaEncomenda> linhas) { this.linhas = linhas; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal total() {
        if (linhas != null && !linhas.isEmpty()) {
            return linhas.stream()
                    .map(LinhaEncomenda::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return total != null ? total : BigDecimal.ZERO;
    }

    public static class LinhaEncomenda {
        private Integer id;
        private String nomeProduto;
        private Integer quantidade;
        private BigDecimal precoUnitario;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getNomeProduto() { return nomeProduto; }
        public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }

        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

        public BigDecimal lineTotal() {
            if (precoUnitario == null || quantidade == null) return BigDecimal.ZERO;
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }
}