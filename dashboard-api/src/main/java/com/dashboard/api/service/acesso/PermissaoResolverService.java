package com.dashboard.api.service.acesso;

import com.dashboard.api.model.acesso.PermissaoEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.model.acesso.UsuarioPermissaoOverride;
import com.dashboard.api.repository.acesso.PermissaoRepository;
import com.dashboard.api.repository.acesso.SetorPermissaoTemplateRepository;
import com.dashboard.api.repository.acesso.UsuarioPapelVinculoRepository;
import com.dashboard.api.repository.acesso.UsuarioPermissaoOverrideRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissaoResolverService {

    private static final String PAPEL_ADMIN_PLATAFORMA = "admin_plataforma";

    private final PermissaoRepository permissaoRepository;
    private final SetorPermissaoTemplateRepository templateRepository;
    private final UsuarioPapelVinculoRepository papelVinculoRepository;
    private final UsuarioPermissaoOverrideRepository overrideRepository;

    public PermissaoResolverService(
            PermissaoRepository permissaoRepository,
            SetorPermissaoTemplateRepository templateRepository,
            UsuarioPapelVinculoRepository papelVinculoRepository,
            UsuarioPermissaoOverrideRepository overrideRepository
    ) {
        this.permissaoRepository = permissaoRepository;
        this.templateRepository = templateRepository;
        this.papelVinculoRepository = papelVinculoRepository;
        this.overrideRepository = overrideRepository;
    }

    /**
     * Calcula permissoes efetivas para um usuario.
     * Retorna Map com chave_legado (camelCase) → boolean, compativel com o frontend.
     */
    public Map<String, Boolean> permissoesEfetivas(UsuarioEntity usuario) {
        List<PermissaoEntity> catalogo = permissaoRepository.findAllByAtivoTrue();

        if (ehAdminPlataforma(usuario.getId())) {
            return catalogoCompleto(catalogo, true);
        }

        // Baseline do setor
        Set<Long> templateIds = templateRepository.findAllBySetorId(usuario.getSetor().getId())
                .stream()
                .map(t -> t.getPermissao().getId())
                .collect(Collectors.toSet());

        // Overrides do usuario
        Map<Long, String> overrides = overrideRepository.findAllByUsuarioId(usuario.getId())
                .stream()
                .collect(Collectors.toMap(
                        o -> o.getPermissao().getId(),
                        UsuarioPermissaoOverride::getTipo
                ));

        Map<String, Boolean> resultado = new LinkedHashMap<>();
        for (PermissaoEntity perm : catalogo) {
            String chave = perm.getChaveLegado() != null ? perm.getChaveLegado() : perm.getChave();
            String override = overrides.get(perm.getId());
            if ("DENY".equals(override)) {
                resultado.put(chave, false);
            } else if ("GRANT".equals(override)) {
                resultado.put(chave, true);
            } else {
                resultado.put(chave, templateIds.contains(perm.getId()));
            }
        }
        return resultado;
    }

    public boolean ehAdminPlataforma(Long usuarioId) {
        return papelVinculoRepository.findAllByUsuarioId(usuarioId)
                .stream()
                .anyMatch(v -> PAPEL_ADMIN_PLATAFORMA.equals(v.getPapel().getNome()));
    }

    public boolean ehAdmin(Long usuarioId) {
        return papelVinculoRepository.findAllByUsuarioId(usuarioId)
                .stream()
                .anyMatch(v -> {
                    String nome = v.getPapel().getNome();
                    return "admin_plataforma".equals(nome) || "admin_acesso".equals(nome);
                });
    }

    public List<String> papeis(Long usuarioId) {
        return papelVinculoRepository.findAllByUsuarioId(usuarioId)
                .stream()
                .map(v -> v.getPapel().getNome())
                .toList();
    }

    private Map<String, Boolean> catalogoCompleto(List<PermissaoEntity> catalogo, boolean valor) {
        Map<String, Boolean> mapa = new LinkedHashMap<>();
        for (PermissaoEntity perm : catalogo) {
            String chave = perm.getChaveLegado() != null ? perm.getChaveLegado() : perm.getChave();
            mapa.put(chave, valor);
        }
        return mapa;
    }
}
