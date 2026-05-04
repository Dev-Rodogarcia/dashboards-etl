package com.dashboard.api.model.acesso;

import jakarta.persistence.*;

@Entity
@Table(name = "setor_permissao_templates", schema = "acesso")
@IdClass(SetorPermissaoTemplateId.class)
public class SetorPermissaoTemplate {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id", nullable = false)
    private SetorEntity setor;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permissao_id", nullable = false)
    private PermissaoEntity permissao;

    public SetorPermissaoTemplate() {}

    public SetorPermissaoTemplate(SetorEntity setor, PermissaoEntity permissao) {
        this.setor = setor;
        this.permissao = permissao;
    }

    public SetorEntity getSetor() { return setor; }
    public void setSetor(SetorEntity setor) { this.setor = setor; }
    public PermissaoEntity getPermissao() { return permissao; }
    public void setPermissao(PermissaoEntity permissao) { this.permissao = permissao; }
}
