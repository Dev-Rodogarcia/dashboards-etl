package com.dashboard.api.service;

import com.dashboard.api.dto.indicadoresgestao.ClienteExcecaoCubagemDTO;
import com.dashboard.api.repository.ClienteExcecaoCubagemSqlRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CubagemClientesExcecaoService {

    private final ClienteExcecaoCubagemSqlRepository repository;
    private final UsuarioAutenticadoContextService usuarioAutenticadoContext;

    public CubagemClientesExcecaoService(
            ClienteExcecaoCubagemSqlRepository repository,
            UsuarioAutenticadoContextService usuarioAutenticadoContext
    ) {
        this.repository = repository;
        this.usuarioAutenticadoContext = usuarioAutenticadoContext;
    }

    @Transactional(readOnly = true)
    public List<ClienteExcecaoCubagemDTO> listar() {
        return repository.listarAtivos();
    }

    @Transactional
    public void excluir(String cnpj) {
        repository.inativarPorCnpj(normalizarCnpj(cnpj), usuarioAutenticadoContext.getNomeComPapel());
    }

    private String normalizarCnpj(String cnpj) {
        String normalizado = cnpj == null ? "" : cnpj.replaceAll("\\D", "");
        if (normalizado.length() != 14) {
            throw new IllegalArgumentException("CNPJ de exceção de cubagem deve conter 14 dígitos.");
        }
        return normalizado;
    }
}
