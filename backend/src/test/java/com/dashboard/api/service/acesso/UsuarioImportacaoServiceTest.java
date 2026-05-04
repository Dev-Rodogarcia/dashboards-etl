package com.dashboard.api.service.acesso;

import com.dashboard.api.dto.acesso.UsuarioAcessoDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoLoteRequestDTO;
import com.dashboard.api.dto.acesso.UsuarioImportacaoSetorResolucaoDTO;
import com.dashboard.api.model.acesso.SetorEntity;
import com.dashboard.api.model.acesso.UsuarioEntity;
import com.dashboard.api.model.acesso.UsuarioImportacaoLoteEntity;
import com.dashboard.api.repository.acesso.PapelRepository;
import com.dashboard.api.repository.acesso.PermissaoRepository;
import com.dashboard.api.repository.acesso.RefreshTokenSessionRepository;
import com.dashboard.api.repository.acesso.AuditLogRepository;
import com.dashboard.api.repository.acesso.SetorRepository;
import com.dashboard.api.repository.acesso.SetorPermissaoTemplateRepository;
import com.dashboard.api.repository.acesso.UsuarioImportacaoLoteRepository;
import com.dashboard.api.repository.acesso.UsuarioPapelVinculoRepository;
import com.dashboard.api.repository.acesso.UsuarioPermissaoOverrideRepository;
import com.dashboard.api.repository.acesso.UsuarioRepository;
import com.dashboard.api.security.IpClienteResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioImportacaoServiceTest {

    @Mock private SetorRepository setorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioImportacaoLoteRepository loteRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private UsuarioImportacaoService service;
    private StubGestaoUsuarioService gestaoUsuarioService;

    @BeforeEach
    void setUp() {
        UsuarioImportacaoExcelParser parser = new UsuarioImportacaoExcelParser();
        UsuarioImportacaoValidator validator = new UsuarioImportacaoValidator(setorRepository, usuarioRepository);
        AuditService auditService = new AuditService(auditLogRepository, new IpClienteResolver(false));
        gestaoUsuarioService = new StubGestaoUsuarioService();
        service = new UsuarioImportacaoService(
                parser,
                validator,
                loteRepository,
                gestaoUsuarioService,
                usuarioRepository,
                auditService,
                new PoliticaSenhaService(),
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preValidarDeveIdentificarSetorInexistenteSemBloquearPreview() throws Exception {
        SetorEntity logistica = criarSetor(1L, "Logística");
        when(setorRepository.findAllByAtivoTrue()).thenReturn(List.of(logistica));
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(usuarioRepository.existsByLoginIgnoreCase(anyString())).thenReturn(false);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "usuarios.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookUsuarios(new String[][]{
                        {"Maria", "maria@empresa.com", "Logística"},
                        {"João", "joao@empresa.com", "Operação"},
                })
        );

        var resposta = service.preValidar(arquivo);

        assertThat(resposta.totais().totalLinhas()).isEqualTo(2);
        assertThat(resposta.totais().validas()).isEqualTo(1);
        assertThat(resposta.setoresInexistentes()).containsExactly("Operação");
        assertThat(resposta.podeImportar()).isFalse();
    }

    @Test
    void importarDeveCriarIgnorarConflitoEReportarErroDeValidacao() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@empresa.com", null, List.of())
        );

        SetorEntity logistica = criarSetor(1L, "Logística");
        UsuarioEntity operador = new UsuarioEntity();
        operador.setId(99L);
        operador.setEmail("admin@empresa.com");
        operador.setLogin("admin@empresa.com");
        operador.setAtivo(true);

        when(setorRepository.findAllByAtivoTrue()).thenReturn(List.of(logistica));
        when(usuarioRepository.findByEmailIgnoreCase("admin@empresa.com")).thenReturn(Optional.of(operador));
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenAnswer(invocation ->
                "jaexiste@empresa.com".equalsIgnoreCase(invocation.getArgument(0))
        );
        when(usuarioRepository.existsByLoginIgnoreCase(anyString())).thenAnswer(invocation ->
                "jaexiste@empresa.com".equalsIgnoreCase(invocation.getArgument(0))
        );
        when(loteRepository.findByTokenImportacao("import-1")).thenReturn(Optional.of(criarLote(
                "import-1",
                new ObjectMapper().writeValueAsString(new LotePayloadFixture(
                        "usuarios.xlsx",
                        List.of(
                                new LinhaPayloadFixture(2, "Maria", "maria@empresa.com", "Logística"),
                                new LinhaPayloadFixture(3, "Existente", "jaexiste@empresa.com", "Logística"),
                                new LinhaPayloadFixture(4, "Inválido", "email-invalido", "Logística")
                        )
                ))
        )));

        gestaoUsuarioService.resposta = new UsuarioAcessoDTO(
                "10",
                "Maria",
                "maria@empresa.com",
                true,
                "1",
                "Logística",
                "usuario_comum",
                Map.of(),
                EscopoFiliaisUsuarioPolicy.HERDAR_SETOR,
                List.of(),
                List.of("Matriz"),
                List.of(),
                List.of(),
                "segura",
                "argon2id"
        );

        var resultado = service.importar(new UsuarioImportacaoLoteRequestDTO("import-1", List.of(
                new UsuarioImportacaoSetorResolucaoDTO("Operação", "1")
        )));

        assertThat(resultado.totalProcessados()).isEqualTo(3);
        assertThat(resultado.totalCriados()).isEqualTo(1);
        assertThat(resultado.totalIgnorados()).isEqualTo(1);
        assertThat(resultado.totalErros()).isEqualTo(1);
        assertThat(resultado.credenciaisTemporarias()).hasSize(1);

        assertThat(gestaoUsuarioService.ultimoRequest).isNotNull();
        assertThat(gestaoUsuarioService.ultimoRequest.papel()).isEqualTo(PermissaoResolverService.PAPEL_USUARIO_COMUM);
        assertThat(gestaoUsuarioService.ultimoRequest.setorId()).isEqualTo("1");
        assertThat(gestaoUsuarioService.ultimoRequest.escopoFiliaisTipo()).isEqualTo(EscopoFiliaisUsuarioPolicy.HERDAR_SETOR);
        assertThat(gestaoUsuarioService.ultimoRequest.filiaisPermitidasUsuario()).isEmpty();
    }

    private static SetorEntity criarSetor(Long id, String nome) {
        SetorEntity setor = new SetorEntity();
        setor.setId(id);
        setor.setNome(nome);
        setor.setFiliaisPermitidas(Set.of("Matriz"));
        return setor;
    }

    private static UsuarioImportacaoLoteEntity criarLote(String token, String payloadJson) {
        UsuarioImportacaoLoteEntity lote = new UsuarioImportacaoLoteEntity();
        lote.setTokenImportacao(token);
        lote.setArquivoNome("usuarios.xlsx");
        lote.setPayloadJson(payloadJson);
        lote.setExpiraEm(Instant.now().plusSeconds(3600));
        return lote;
    }

    private static byte[] workbookUsuarios(String[][] linhas) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Usuários");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nome do Usuário");
            header.createCell(1).setCellValue("E-mail");
            header.createCell(2).setCellValue("Setor");

            for (int index = 0; index < linhas.length; index++) {
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(linhas[index][0]);
                row.createCell(1).setCellValue(linhas[index][1]);
                row.createCell(2).setCellValue(linhas[index][2]);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private record LotePayloadFixture(
            String arquivoNome,
            List<LinhaPayloadFixture> linhas
    ) {
    }

    private record LinhaPayloadFixture(
            int linha,
            String nome,
            String email,
            String setor
    ) {
    }

    private static final class StubGestaoUsuarioService extends GestaoUsuarioService {
        private com.dashboard.api.dto.acesso.UsuarioRequestDTO ultimoRequest;
        private UsuarioAcessoDTO resposta;

        private StubGestaoUsuarioService() {
            super(
                    mock(UsuarioRepository.class),
                    mock(SetorRepository.class),
                    mock(PapelRepository.class),
                    mock(UsuarioPapelVinculoRepository.class),
                    mock(UsuarioPermissaoOverrideRepository.class),
                    mock(PermissaoRepository.class),
                    new PasswordHashService(
                            org.springframework.security.crypto.argon2.Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                    ),
                    new PermissaoResolverService(
                            mock(PermissaoRepository.class),
                            mock(SetorPermissaoTemplateRepository.class),
                            mock(UsuarioPapelVinculoRepository.class),
                            mock(UsuarioPermissaoOverrideRepository.class),
                            new UsuarioSupremo("supremo@empresa.com", "Senha@123456", "Supremo", "desenvolvedor", 1000, false)
                    ),
                    new AuditService(mock(AuditLogRepository.class), new IpClienteResolver(false)),
                    new PoliticaSenhaService(),
                    new RefreshTokenService(mock(RefreshTokenSessionRepository.class), 24),
                    mock(UsuarioDependenciaCleanup.class),
                    new UsuarioSupremo("supremo@empresa.com", "Senha@123456", "Supremo", "desenvolvedor", 1000, false),
                    new StubEscopoFiliaisUsuarioStore()
            );
        }

        @Override
        public UsuarioAcessoDTO criarUsuario(com.dashboard.api.dto.acesso.UsuarioRequestDTO request) {
            this.ultimoRequest = request;
            return resposta;
        }
    }

    private static final class StubEscopoFiliaisUsuarioStore extends EscopoFiliaisUsuarioStore {
        private StubEscopoFiliaisUsuarioStore() {
            super(null);
        }

        @Override
        public void carregarNoUsuario(com.dashboard.api.model.acesso.UsuarioEntity usuario) {
        }

        @Override
        public void salvar(com.dashboard.api.model.acesso.UsuarioEntity usuario) {
        }
    }
}
