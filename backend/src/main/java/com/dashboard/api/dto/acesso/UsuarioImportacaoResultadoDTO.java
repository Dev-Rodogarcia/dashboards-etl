package com.dashboard.api.dto.acesso;

import java.util.List;

public record UsuarioImportacaoResultadoDTO(
        int totalProcessados,
        int totalCriados,
        int totalIgnorados,
        int totalErros,
        List<UsuarioImportacaoCriadoDTO> listaCriados,
        List<UsuarioImportacaoIgnoradoDTO> listaIgnorados,
        List<UsuarioImportacaoErroDTO> listaErros,
        List<UsuarioImportacaoCredencialTemporariaDTO> credenciaisTemporarias
) {
}
