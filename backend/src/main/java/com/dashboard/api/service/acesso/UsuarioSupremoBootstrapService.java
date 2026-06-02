package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.PapelEntity;
import com.dashboard.api.model.acesso.SetorEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.model.acesso.UsuarioPapelVinculo;
import com.dashboard.api.repository.acesso.PapelRepository;
import com.dashboard.api.repository.acesso.SetorRepository;
import com.dashboard.api.repository.acesso.UsuarioPapelVinculoRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.security.acesso.UsuarioSupremo;
import com.dashboard.api.service.acesso.PasswordHashService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(6)
public class UsuarioSupremoBootstrapService implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;
    private final PapelRepository papelRepository;
    private final UsuarioPapelVinculoRepository papelVinculoRepository;
    private final PasswordHashService passwordHashService;
    private final UsuarioSupremo usuarioSupremo;
    private final JdbcTemplate jdbcTemplate;

    public UsuarioSupremoBootstrapService(
            UsuarioRepository usuarioRepository,
            SetorRepository setorRepository,
            PapelRepository papelRepository,
            UsuarioPapelVinculoRepository papelVinculoRepository,
            PasswordHashService passwordHashService,
            UsuarioSupremo usuarioSupremo,
            JdbcTemplate jdbcTemplate
    ) {
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.papelRepository = papelRepository;
        this.papelVinculoRepository = papelVinculoRepository;
        this.passwordHashService = passwordHashService;
        this.usuarioSupremo = usuarioSupremo;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        garantirUsuarioSupremo();
    }

    private void garantirUsuarioSupremo() {
        SetorEntity setor = garantirSetorAdmin();
        PapelEntity papel = garantirPapelDesenvolvedor();

        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(usuarioSupremo.email()).orElseGet(UsuarioEntity::new);
        boolean novoUsuario = usuario.getId() == null;
        if (novoUsuario || usuarioSupremo.rotacionarSenha()) {
            PasswordHashService.PasswordHash senhaHash = passwordHashService.gerarHashSeguro(usuarioSupremo.senhaInicial());
            usuario.setSenhaHash(senhaHash.valor());
            usuario.setAlgoritmoHash(senhaHash.algoritmo());
            usuario.setSenhaAlteradaEm(Instant.now());
        }

        usuario.setNome(usuarioSupremo.nome());
        usuario.setEmail(usuarioSupremo.email());
        usuario.setLogin(usuarioSupremo.email());
        usuario.setExigeTrocaSenha(false);
        usuario.setTentativasFalha(0);
        usuario.setBloqueadoAte(null);
        usuario.setIdentitySource("local");
        usuario.setMfaStatus("disabled");
        usuario.setSetor(setor);
        usuario.setAtivo(true);
        usuario = usuarioRepository.save(usuario);

        garantirVinculoDesenvolvedor(usuario, papel);
        registrarMetadadosUsuarioSupremo(usuario, papel);
    }

    private void garantirVinculoDesenvolvedor(UsuarioEntity usuario, PapelEntity papel) {
        Long usuarioId = Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.");
        List<UsuarioPapelVinculo> vinculos = papelVinculoRepository.findAllByUsuarioId(usuarioId);

        UsuarioPapelVinculo vinculoDesenvolvedor = vinculos.stream()
                .filter(this::ehVinculoDesenvolvedor)
                .findFirst()
                .orElse(null);

        if (vinculoDesenvolvedor == null && !vinculos.isEmpty()) {
            vinculoDesenvolvedor = vinculos.get(0);
            vinculoDesenvolvedor.setPapel(papel);
            papelVinculoRepository.saveAndFlush(vinculoDesenvolvedor);
        } else if (vinculoDesenvolvedor == null) {
            vinculoDesenvolvedor = new UsuarioPapelVinculo();
            vinculoDesenvolvedor.setUsuario(usuario);
            vinculoDesenvolvedor.setPapel(papel);
            papelVinculoRepository.saveAndFlush(vinculoDesenvolvedor);
        }

        Long vinculoMantidoId = vinculoDesenvolvedor.getId();
        papelVinculoRepository.findAllByUsuarioId(usuarioId).stream()
                .filter(vinculo -> !Objects.equals(vinculo.getId(), vinculoMantidoId))
                .forEach(papelVinculoRepository::delete);
        papelVinculoRepository.flush();
    }

    private boolean ehVinculoDesenvolvedor(UsuarioPapelVinculo vinculo) {
        return vinculo.getPapel() != null
                && usuarioSupremo.papel().equalsIgnoreCase(vinculo.getPapel().getNome());
    }

    private SetorEntity garantirSetorAdmin() {
        SetorEntity setor = setorRepository.findByChave("setor-admin").orElseGet(SetorEntity::new);
        setor.setChave("setor-admin");
        setor.setNome("TI - Admin");
        setor.setDescricao("Área com acesso total ao sistema");
        setor.setSistema(true);
        setor.setAtivo(true);
        return setorRepository.save(setor);
    }

    private PapelEntity garantirPapelDesenvolvedor() {
        PapelEntity papel = papelRepository.findByNome(usuarioSupremo.papel()).orElseGet(PapelEntity::new);
        papel.setNome(usuarioSupremo.papel());
        papel.setDescricao("Nível supremo e imutável do projeto Dashboards");
        papel.setNivel(usuarioSupremo.nivel());
        papel.setAtivo(true);
        return papelRepository.save(papel);
    }

    private void registrarMetadadosUsuarioSupremo(UsuarioEntity usuario, PapelEntity papel) {
        if (!tabelaExiste("acesso.configuracoes_seguranca")) {
            return;
        }

        gravarConfiguracao("usuario_supremo_id", String.valueOf(Objects.requireNonNull(usuario.getId(), "usuario.id é obrigatório.")));
        gravarConfiguracao("papel_supremo_id", String.valueOf(Objects.requireNonNull(papel.getId(), "papel.id é obrigatório.")));
    }

    private void gravarConfiguracao(String chave, String valor) {
        jdbcTemplate.update("""
                MERGE acesso.configuracoes_seguranca AS alvo
                USING (SELECT ? AS chave, ? AS valor) AS origem
                   ON alvo.chave = origem.chave
                WHEN MATCHED THEN
                    UPDATE SET valor = origem.valor, atualizado_em = SYSUTCDATETIME()
                WHEN NOT MATCHED THEN
                    INSERT (chave, valor) VALUES (origem.chave, origem.valor);
                """, chave, valor);
    }

    private boolean tabelaExiste(String nomeCompletoTabela) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) WHERE OBJECT_ID(?, 'U') IS NOT NULL",
                Integer.class,
                nomeCompletoTabela
        );
        return total != null && total > 0;
    }
}
