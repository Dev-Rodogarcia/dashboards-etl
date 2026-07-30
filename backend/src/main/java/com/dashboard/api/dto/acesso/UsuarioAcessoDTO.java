package com.dashboard.api.dto.acesso;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record UsuarioAcessoDTO(
        String id,
        String nome,
        String email,
        boolean ativo,
        String setorId,
        String setorNome,
        String papel,
        Map<String, Boolean> permissoesEfetivas,
        String escopoFiliaisTipo,
        List<String> filiaisPermitidasUsuario,
        List<String> filiaisPermitidasEfetivas,
        List<String> permissoesNegadas,
        List<String> permissoesConcedidas,
        String statusSenha,
        String algoritmoSenha,
        Instant passwordResetRequestedAt,
        @JsonProperty("isOnline") boolean online,
        OffsetDateTime ultimaAtividade,
        String ultimaRotaAcessada
) {
    public UsuarioAcessoDTO(
            String id,
            String nome,
            String email,
            boolean ativo,
            String setorId,
            String setorNome,
            String papel,
            Map<String, Boolean> permissoesEfetivas,
            String escopoFiliaisTipo,
            List<String> filiaisPermitidasUsuario,
            List<String> filiaisPermitidasEfetivas,
            List<String> permissoesNegadas,
            List<String> permissoesConcedidas,
            String statusSenha,
            String algoritmoSenha,
            boolean online,
            OffsetDateTime ultimaAtividade,
            String ultimaRotaAcessada
    ) {
        this(
                id,
                nome,
                email,
                ativo,
                setorId,
                setorNome,
                papel,
                permissoesEfetivas,
                escopoFiliaisTipo,
                filiaisPermitidasUsuario,
                filiaisPermitidasEfetivas,
                permissoesNegadas,
                permissoesConcedidas,
                statusSenha,
                algoritmoSenha,
                null,
                online,
                ultimaAtividade,
                ultimaRotaAcessada
        );
    }
}
