package com.dashboard.api.model.acesso;

import java.io.Serializable;
import java.util.Objects;

public class SetorPermissaoTemplateId implements Serializable {

    private Long setor;
    private Long permissao;

    public SetorPermissaoTemplateId() {}

    public SetorPermissaoTemplateId(Long setor, Long permissao) {
        this.setor = setor;
        this.permissao = permissao;
    }

    public Long getSetor() { return setor; }
    public void setSetor(Long setor) { this.setor = setor; }
    public Long getPermissao() { return permissao; }
    public void setPermissao(Long permissao) { this.permissao = permissao; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetorPermissaoTemplateId that)) return false;
        return Objects.equals(setor, that.setor) && Objects.equals(permissao, that.permissao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(setor, permissao);
    }
}
