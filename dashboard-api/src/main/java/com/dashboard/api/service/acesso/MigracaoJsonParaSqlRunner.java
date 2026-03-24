package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.*;
import com.dashboard.api.repository.acesso.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(10)
public class MigracaoJsonParaSqlRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoJsonParaSqlRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;
    private final PermissaoRepository permissaoRepository;
    private final PapelRepository papelRepository;
    private final SetorPermissaoTemplateRepository templateRepository;
    private final UsuarioPapelVinculoRepository papelVinculoRepository;
    private final Path storagePath;
    private final boolean migrationEnabled;

    public MigracaoJsonParaSqlRunner(
            UsuarioRepository usuarioRepository,
            SetorRepository setorRepository,
            PermissaoRepository permissaoRepository,
            PapelRepository papelRepository,
            SetorPermissaoTemplateRepository templateRepository,
            UsuarioPapelVinculoRepository papelVinculoRepository,
            @Value("${acl.storage-file:./storage/access-control.json}") String storageFile,
            @Value("${acl.legacy.migration-enabled:false}") boolean migrationEnabled
    ) {
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.permissaoRepository = permissaoRepository;
        this.papelRepository = papelRepository;
        this.templateRepository = templateRepository;
        this.papelVinculoRepository = papelVinculoRepository;
        this.storagePath = Path.of(storageFile).toAbsolutePath().normalize();
        this.migrationEnabled = migrationEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!migrationEnabled) {
            if (Files.exists(storagePath)) {
                log.warn("Migração JSON→SQL: desabilitada. O arquivo legado {} será ignorado até habilitação explícita.", storagePath);
            } else {
                log.info("Migração JSON→SQL: desabilitada por configuração.");
            }
            return;
        }

        // Pular se ja existem usuarios no banco (migracao ja feita)
        if (usuarioRepository.count() > 0) {
            log.info("Migração JSON→SQL: ignorada — banco já possui usuários.");
            return;
        }

        // Pular se o arquivo JSON nao existe
        if (!Files.exists(storagePath)) {
            log.info("Migração JSON→SQL: ignorada — arquivo {} não encontrado.", storagePath);
            return;
        }

        try {
            log.info("Migração JSON→SQL: iniciando a partir de {}", storagePath);
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            AccessControlStore store = mapper.readValue(storagePath.toFile(), AccessControlStore.class);

            int setoresMigrados = migrarSetores(store.setores);
            int usuariosMigrados = migrarUsuarios(store.usuarios);

            // Renomear arquivo como backup
            Path backup = storagePath.resolveSibling(storagePath.getFileName() + ".migrated");
            Files.move(storagePath, backup);

            log.info("Migração JSON→SQL: concluída — {} setores, {} usuários migrados. Backup em {}",
                    setoresMigrados, usuariosMigrados, backup);

        } catch (Exception ex) {
            log.error("Migração JSON→SQL: FALHA — {}", ex.getMessage(), ex);
        }
    }

    private int migrarSetores(List<StoredSector> setores) {
        int count = 0;
        for (StoredSector stored : setores) {
            // Verificar se ja existe por chave (seed SQL pode ter criado)
            SetorEntity setor = setorRepository.findByChave(stored.id).orElse(null);
            if (setor == null) {
                setor = new SetorEntity();
                setor.setChave(stored.id);
                setor.setNome(stored.nome);
                setor.setDescricao(stored.descricao);
                setor.setSistema(stored.sistema);
                setor = setorRepository.save(setor);
            }

            // Sincronizar permissoes do template
            if (stored.permissoes != null) {
                SetorEntity finalSetor = setor;
                for (Map.Entry<String, Boolean> entry : stored.permissoes.entrySet()) {
                    if (Boolean.TRUE.equals(entry.getValue())) {
                        permissaoRepository.findByChaveLegado(entry.getKey()).ifPresent(perm -> {
                            SetorPermissaoTemplateId tid = new SetorPermissaoTemplateId(finalSetor.getId(), perm.getId());
                            if (!templateRepository.existsById(tid)) {
                                templateRepository.save(new SetorPermissaoTemplate(finalSetor, perm));
                            }
                        });
                    }
                }
            }
            count++;
        }
        return count;
    }

    private int migrarUsuarios(List<StoredUser> usuarios) {
        int count = 0;
        for (StoredUser stored : usuarios) {
            // Pular se login ja existe
            if (usuarioRepository.existsByLoginIgnoreCase(stored.login)) {
                log.info("Migração: usuário '{}' já existe no banco, ignorando.", stored.login);
                continue;
            }

            SetorEntity setor = setorRepository.findByChave(stored.setorId).orElse(null);
            if (setor == null) {
                log.warn("Migração: setor '{}' não encontrado para usuário '{}', ignorando.", stored.setorId, stored.login);
                continue;
            }

            UsuarioEntity usuario = new UsuarioEntity();
            usuario.setChaveLegado(stored.id);
            usuario.setLogin(stored.login);
            usuario.setNome(stored.nome);
            usuario.setEmail(stored.email);
            usuario.setSenhaHash(stored.senhaHash);
            usuario.setSetor(setor);
            usuario.setAtivo(stored.ativo);
            usuario.setExigeTrocaSenha(true);
            usuario = usuarioRepository.save(usuario);

            // Atribuir papel
            String nomePapel = stored.admin ? "admin_plataforma" : "usuario_comum";
            UsuarioEntity finalUsuario = usuario;
            papelRepository.findByNome(nomePapel).ifPresent(papel -> {
                UsuarioPapelVinculo vinculo = new UsuarioPapelVinculo();
                vinculo.setUsuario(finalUsuario);
                vinculo.setPapel(papel);
                papelVinculoRepository.save(vinculo);
            });

            count++;
        }
        return count;
    }

    // Classes internas para desserializar o JSON legado
    private static class AccessControlStore {
        public List<StoredSector> setores = new ArrayList<>();
        public List<StoredUser> usuarios = new ArrayList<>();
    }

    private static class StoredSector {
        public String id;
        public String nome;
        public String descricao;
        public boolean sistema;
        public Map<String, Boolean> permissoes = new LinkedHashMap<>();
        @SuppressWarnings("unused")
        public LocalDateTime atualizadoEm;
    }

    private static class StoredUser {
        public String id;
        public String login;
        public String nome;
        public String email;
        public String senhaHash;
        public String setorId;
        public boolean admin;
        public boolean ativo;
        @SuppressWarnings("unused")
        public LocalDateTime atualizadoEm;
    }
}
