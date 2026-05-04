package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.PermissaoEntity;
import com.dashboard.api.repository.acesso.PermissaoRepository;
import com.dashboard.api.security.PermissaoCatalogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Component
@Order(5)
public class PermissaoCatalogoBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissaoCatalogoBootstrapService.class);

    private final PermissaoRepository permissaoRepository;

    public PermissaoCatalogoBootstrapService(PermissaoRepository permissaoRepository) {
        this.permissaoRepository = permissaoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Arrays.stream(PermissaoCatalogo.values()).forEach(this::sincronizarPermissao);
    }

    private void sincronizarPermissao(PermissaoCatalogo catalogo) {
        PermissaoEntity permissao = permissaoRepository.findByChaveLegado(catalogo.chave())
                .or(() -> permissaoRepository.findByChave("dashboard." + normalizarRecurso(catalogo) + ".read"))
                .orElseGet(PermissaoEntity::new);

        permissao.setChave("dashboard." + normalizarRecurso(catalogo) + ".read");
        permissao.setChaveLegado(catalogo.chave());
        permissao.setNome(catalogo.nome());
        permissao.setDescricao(catalogo.descricao());
        permissao.setRecurso(normalizarRecurso(catalogo));
        permissao.setAcao("read");
        permissao.setRota(catalogo.rota());
        permissao.setAtivo(true);

        permissaoRepository.save(permissao);
        log.debug("Permissão sincronizada: {}", catalogo.chave());
    }

    private String normalizarRecurso(PermissaoCatalogo catalogo) {
        return switch (catalogo) {
            case FATURAS_POR_CLIENTE -> "faturas_por_cliente";
            case CONTAS_A_PAGAR -> "contas_a_pagar";
            case ETL_SAUDE -> "etl_saude";
            case INDICADORES_GESTAO_A_VISTA -> "indicadores_gestao_a_vista";
            default -> catalogo.chave();
        };
    }
}
