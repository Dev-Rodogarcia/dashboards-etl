package com.dashboard.api.service;

import com.dashboard.api.dto.apresentacao.ApresentacaoSequenciaDTO;
import com.dashboard.api.dto.apresentacao.ApresentacaoSequenciaRequestDTO;
import com.dashboard.api.model.acesso.ApresentacaoSequenciaEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.repository.acesso.ApresentacaoSequenciaRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.service.acesso.PermissaoResolverService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApresentacaoSequenciaService {
    private static final Map<String, String> PERMISSAO_POR_PAGINA = Map.ofEntries(
            Map.entry("faturamento", "fretes"), Map.entry("cotacoes", "cotacoes"),
            Map.entry("coletas", "coletas"), Map.entry("performance", "performance"),
            Map.entry("manifestos", "manifestos"), Map.entry("indicadores-gestao-a-vista", "indicadoresGestaoAVista"));
    private final ApresentacaoSequenciaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PermissaoResolverService permissaoResolver;
    public ApresentacaoSequenciaService(ApresentacaoSequenciaRepository repository, UsuarioRepository usuarioRepository, PermissaoResolverService permissaoResolver) {
        this.repository = repository; this.usuarioRepository = usuarioRepository; this.permissaoResolver = permissaoResolver;
    }
    @Transactional(readOnly = true)
    public List<ApresentacaoSequenciaDTO> listar(String email) {
        UsuarioEntity usuario = usuario(email);
        return repository.findByUsuarioIdAndAtivoTrueOrderByAtualizadoEmDesc(usuario.getId()).stream().map(sequencia -> dto(sequencia, autorizadas(usuario))).toList();
    }
    @Transactional
    public ApresentacaoSequenciaDTO criar(ApresentacaoSequenciaRequestDTO request, String email) {
        UsuarioEntity usuario = usuario(email); List<String> paginas = validar(request.paginas(), autorizadas(usuario));
        ApresentacaoSequenciaEntity sequencia = new ApresentacaoSequenciaEntity(); sequencia.setUsuario(usuario); sequencia.setNome(request.nome().trim()); sequencia.substituirItens(paginas);
        return dto(repository.save(sequencia), paginas);
    }
    @Transactional
    public ApresentacaoSequenciaDTO atualizar(Long id, ApresentacaoSequenciaRequestDTO request, String email) {
        UsuarioEntity usuario = usuario(email); List<String> paginas = validar(request.paginas(), autorizadas(usuario));
        ApresentacaoSequenciaEntity sequencia = repository.findByIdAndUsuarioIdAndAtivoTrue(id, usuario.getId()).orElseThrow(() -> naoEncontrada());
        sequencia.setNome(request.nome().trim()); sequencia.substituirItens(paginas); return dto(sequencia, paginas);
    }
    @Transactional
    public void inativar(Long id, String email) { UsuarioEntity usuario = usuario(email); ApresentacaoSequenciaEntity sequencia = repository.findByIdAndUsuarioIdAndAtivoTrue(id, usuario.getId()).orElseThrow(() -> naoEncontrada()); sequencia.setAtivo(false); }
    private UsuarioEntity usuario(String email) { return usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
    private Set<String> autorizadas(UsuarioEntity usuario) { Map<String, Boolean> permissoes = permissaoResolver.permissoesEfetivas(usuario); return PERMISSAO_POR_PAGINA.entrySet().stream().filter(item -> Boolean.TRUE.equals(permissoes.get(item.getValue()))).map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()); }
    private List<String> validar(List<String> paginas, Set<String> autorizadas) { List<String> normalizadas = paginas.stream().map(String::trim).map(String::toLowerCase).toList(); if (normalizadas.stream().anyMatch(pagina -> !PERMISSAO_POR_PAGINA.containsKey(pagina) || !autorizadas.contains(pagina)) || normalizadas.size() != new java.util.HashSet<>(normalizadas).size()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A sequência contém página não autorizada."); return normalizadas; }
    private ApresentacaoSequenciaDTO dto(ApresentacaoSequenciaEntity sequencia, Set<String> autorizadas) { return dto(sequencia, sequencia.getItens().stream().map(item -> item.getPagina()).filter(autorizadas::contains).toList()); }
    private ApresentacaoSequenciaDTO dto(ApresentacaoSequenciaEntity sequencia, List<String> paginas) { return new ApresentacaoSequenciaDTO(sequencia.getId(), sequencia.getNome(), paginas); }
    private ResponseStatusException naoEncontrada() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Sequência não encontrada."); }
}
