package com.dashboard.api.service;

import com.dashboard.api.repository.ClienteExcecaoCubagemSqlRepository;
import com.dashboard.api.repository.ClienteExcecaoCubagemSqlRepository.ClienteExcecaoCubagemRegistro;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;

class CubagemClientesExcecaoImportacaoServiceTest {

    @Test
    void importarDeveEnviarSomenteLinhasValidasComCnpjLimpoParaMerge() {
        FakeClienteExcecaoCubagemSqlRepository repository = new FakeClienteExcecaoCubagemSqlRepository();
        CubagemClientesExcecaoImportacaoService service = new CubagemClientesExcecaoImportacaoService(
                new CubagemClientesExcecaoImportacaoParser(),
                repository,
                usuarioAutenticado("Operador | admin_plataforma")
        );
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "clientes-sem-cubagem.csv",
                "text/csv",
                ("CNPJ;Razão Social\n"
                        + "43.996.693/0001-27;Cliente Valido\n"
                        + "123;Cliente Invalido\n").getBytes(StandardCharsets.UTF_8)
        );

        var resultado = service.importar(arquivo);

        assertThat(resultado.totalProcessados()).isEqualTo(2);
        assertThat(resultado.totalImportados()).isEqualTo(1);
        assertThat(resultado.totalErros()).isEqualTo(1);
        assertThat(repository.atualizadoPor).isEqualTo("Operador | admin_plataforma");
        assertThat(repository.registros)
                .extracting(ClienteExcecaoCubagemRegistro::clienteCnpj)
                .containsExactly("43996693000127");
    }

    @Test
    void preValidarDeveBloquearCnpjDuplicadoNaPlanilha() {
        CubagemClientesExcecaoImportacaoService service = new CubagemClientesExcecaoImportacaoService(
                new CubagemClientesExcecaoImportacaoParser(),
                new FakeClienteExcecaoCubagemSqlRepository(),
                usuarioAutenticado("Operador | admin_plataforma")
        );
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "clientes-sem-cubagem.csv",
                "text/csv",
                ("CNPJ;Razão Social\n"
                        + "43.996.693/0001-27;Cliente A\n"
                        + "43996693000127;Cliente B\n").getBytes(StandardCharsets.UTF_8)
        );

        var preview = service.preValidar(arquivo);

        assertThat(preview.podeImportar()).isFalse();
        assertThat(preview.totais().invalidas()).isEqualTo(2);
        assertThat(preview.linhasPreview())
                .allSatisfy(linha -> assertThat(linha.mensagens()).contains("CNPJ duplicado na planilha."));
    }

    private static UsuarioAutenticadoContextService usuarioAutenticado(String nomeComPapel) {
        return new UsuarioAutenticadoContextService(null, null) {
            @Override
            public String getNomeComPapel() {
                return nomeComPapel;
            }
        };
    }

    private static final class FakeClienteExcecaoCubagemSqlRepository extends ClienteExcecaoCubagemSqlRepository {

        private List<ClienteExcecaoCubagemRegistro> registros = List.of();
        private String atualizadoPor;

        private FakeClienteExcecaoCubagemSqlRepository() {
            super(new NamedParameterJdbcTemplate(new JdbcTemplate()));
        }

        @Override
        public void merge(List<ClienteExcecaoCubagemRegistro> registros, String atualizadoPor) {
            this.registros = List.copyOf(registros);
            this.atualizadoPor = atualizadoPor;
        }
    }
}
