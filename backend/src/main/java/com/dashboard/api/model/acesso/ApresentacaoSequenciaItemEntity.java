package com.dashboard.api.model.acesso;

import jakarta.persistence.*;

@Entity
@Table(name = "apresentacao_sequencia_itens", schema = "acesso")
public class ApresentacaoSequenciaItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sequencia_id", nullable = false) private ApresentacaoSequenciaEntity sequencia;
    @Column(nullable = false, length = 60) private String pagina;
    @Column(nullable = false) private int ordem;
    public Long getId() { return id; }
    public ApresentacaoSequenciaEntity getSequencia() { return sequencia; }
    public void setSequencia(ApresentacaoSequenciaEntity sequencia) { this.sequencia = sequencia; }
    public String getPagina() { return pagina; }
    public void setPagina(String pagina) { this.pagina = pagina; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
