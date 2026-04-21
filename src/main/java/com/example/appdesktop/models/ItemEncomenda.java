package com.example.appdesktop.models;

import java.math.BigDecimal;

public class ItemEncomenda {

    private Integer id;
    private EncomendaCatalogo idEncomenda;
    private ArtigoRef idArtigo;
    private Integer quantidade;
    private String nomeProduto;
    private BigDecimal precoUnitario;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EncomendaCatalogo getIdEncomenda() {
        return idEncomenda;
    }

    public void setIdEncomenda(EncomendaCatalogo idEncomenda) {
        this.idEncomenda = idEncomenda;
    }

    public ArtigoRef getIdArtigo() {
        return idArtigo;
    }

    public void setIdArtigo(ArtigoRef idArtigo) {
        this.idArtigo = idArtigo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public String resolvedNomeProduto() {
        if (nomeProduto != null && !nomeProduto.isBlank()) {
            return nomeProduto;
        }
        if (idArtigo != null) {
            return idArtigo.resolvedNome();
        }
        return "Produto";
    }

    public BigDecimal resolvedPrecoUnitario() {
        if (precoUnitario != null) {
            return precoUnitario;
        }
        if (idArtigo != null) {
            return idArtigo.resolvedPreco();
        }
        return BigDecimal.ZERO;
    }

    public static class ArtigoRef {
        private Integer id;
        private String nome;
        private String nomeArtigo;
        private String titulo;
        private BigDecimal precoUnitario;
        private BigDecimal preco;
        private BigDecimal precoVenda;

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

        public String getNomeArtigo() {
            return nomeArtigo;
        }

        public void setNomeArtigo(String nomeArtigo) {
            this.nomeArtigo = nomeArtigo;
        }

        public String getTitulo() {
            return titulo;
        }

        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }

        public BigDecimal getPrecoUnitario() {
            return precoUnitario;
        }

        public void setPrecoUnitario(BigDecimal precoUnitario) {
            this.precoUnitario = precoUnitario;
        }

        public BigDecimal getPreco() {
            return preco;
        }

        public void setPreco(BigDecimal preco) {
            this.preco = preco;
        }

        public BigDecimal getPrecoVenda() {
            return precoVenda;
        }

        public void setPrecoVenda(BigDecimal precoVenda) {
            this.precoVenda = precoVenda;
        }

        public String resolvedNome() {
            if (nome != null && !nome.isBlank()) {
                return nome;
            }
            if (nomeArtigo != null && !nomeArtigo.isBlank()) {
                return nomeArtigo;
            }
            if (titulo != null && !titulo.isBlank()) {
                return titulo;
            }
            return "Produto";
        }

        public BigDecimal resolvedPreco() {
            if (precoUnitario != null) {
                return precoUnitario;
            }
            if (preco != null) {
                return preco;
            }
            if (precoVenda != null) {
                return precoVenda;
            }
            return BigDecimal.ZERO;
        }
    }
}
