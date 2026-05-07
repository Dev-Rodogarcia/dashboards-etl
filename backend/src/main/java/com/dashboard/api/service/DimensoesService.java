package com.dashboard.api.service;

import com.dashboard.api.dto.dimensoes.PlanoContasDimDTO;
import com.dashboard.api.dto.dimensoes.UsuarioDimDTO;
import com.dashboard.api.dto.dimensoes.VeiculoDimDTO;
import com.dashboard.api.model.DimUsuarioEntity;
import com.dashboard.api.model.DimVeiculoEntity;
import com.dashboard.api.model.VisaoManifestosEntity;
import com.dashboard.api.repository.DimFilialRepository;
import com.dashboard.api.repository.DimUsuarioRepository;
import com.dashboard.api.repository.DimVeiculoRepository;
import com.dashboard.api.repository.VisaoColetasRepository;
import com.dashboard.api.repository.VisaoContasAPagarRepository;
import com.dashboard.api.repository.VisaoCotacoesRepository;
import com.dashboard.api.repository.VisaoFaturasClienteRepository;
import com.dashboard.api.repository.VisaoFretesRepository;
import com.dashboard.api.repository.VisaoManifestosRepository;
import com.dashboard.api.service.acesso.EscopoFilialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DimensoesService {

    private static final Logger log = LoggerFactory.getLogger(DimensoesService.class);

    private final DimFilialRepository dimFilialRepository;
    private final DimUsuarioRepository dimUsuarioRepository;
    private final DimVeiculoRepository dimVeiculoRepository;
    private final VisaoColetasRepository coletasRepository;
    private final VisaoFretesRepository fretesRepository;
    private final VisaoCotacoesRepository cotacoesRepository;
    private final VisaoFaturasClienteRepository faturasClienteRepository;
    private final VisaoManifestosRepository manifestosRepository;
    private final VisaoContasAPagarRepository contasAPagarRepository;
    private final EscopoFilialService escopoFilialService;

    public DimensoesService(
            DimFilialRepository dimFilialRepository,
            DimUsuarioRepository dimUsuarioRepository,
            DimVeiculoRepository dimVeiculoRepository,
            VisaoColetasRepository coletasRepository,
            VisaoFretesRepository fretesRepository,
            VisaoCotacoesRepository cotacoesRepository,
            VisaoFaturasClienteRepository faturasClienteRepository,
            VisaoManifestosRepository manifestosRepository,
            VisaoContasAPagarRepository contasAPagarRepository,
            EscopoFilialService escopoFilialService
    ) {
        this.dimFilialRepository = dimFilialRepository;
        this.dimUsuarioRepository = dimUsuarioRepository;
        this.dimVeiculoRepository = dimVeiculoRepository;
        this.coletasRepository = coletasRepository;
        this.fretesRepository = fretesRepository;
        this.cotacoesRepository = cotacoesRepository;
        this.faturasClienteRepository = faturasClienteRepository;
        this.manifestosRepository = manifestosRepository;
        this.contasAPagarRepository = contasAPagarRepository;
        this.escopoFilialService = escopoFilialService;
    }

    public List<String> listarFiliais() {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        if (escopo.acessoTotal()) {
            return dimFilialRepository.findAll().stream()
                    .map(e -> e.getNomeFilial())
                    .filter(this::temTexto)
                    .map(String::trim)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return escopo.filiaisOrdenadas();
    }

    public List<String> listarClientes() {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        Set<String> clientes = new LinkedHashSet<>();

        coletasRepository.findAll().stream()
                .filter(row -> escopo.permiteAlgumaFilial(row.getFilialNome()))
                .map(row -> row.getClienteNome())
                .filter(this::temTexto)
                .map(String::trim)
                .forEach(clientes::add);

        fretesRepository.findAll().stream()
                .filter(row -> escopo.permiteAlgumaFilial(row.getFilialNome()))
                .flatMap(row -> textoStream(row.getPagadorNome(), row.getRemetenteNome(), row.getDestinatarioNome()))
                .forEach(clientes::add);

        cotacoesRepository.findAll().stream()
                .filter(row -> escopo.permiteAlgumaFilial(row.getFilial()))
                .flatMap(row -> textoStream(row.getClientePagador(), row.getCliente()))
                .forEach(clientes::add);

        try {
            faturasClienteRepository.findAll().stream()
                    .filter(row -> escopo.permiteAlgumaFilial(row.getFilial()))
                    .flatMap(row -> textoStream(row.getPagadorNome(), row.getRemetenteNome(), row.getDestinatarioNome()))
                    .forEach(clientes::add);
        } catch (DataAccessException ex) {
            log.warn("Dimensão de clientes ignorou faturas_por_cliente temporariamente. Verifique a view vw_faturas_por_cliente_powerbi. Causa: {}",
                    ex.getMostSpecificCause().getMessage());
        }

        return clientes.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> listarClientesCnpjFaturasPorCliente() {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        try {
            if (escopo.acessoTotal()) {
                return faturasClienteRepository.findDistinctClienteCnpj();
            }

            List<String> filiais = escopo.filiaisOrdenadas().stream()
                    .filter(this::temTexto)
                    .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
                    .toList();
            if (filiais.isEmpty()) {
                return List.of();
            }
            return faturasClienteRepository.findDistinctClienteCnpjByFilialIn(filiais);
        } catch (DataAccessException ex) {
            log.warn("Dimensão Cliente/CNPJ indisponível. Verifique se a view vw_faturas_por_cliente_powerbi expõe [Cliente/CNPJ]. Causa: {}",
                    ex.getMostSpecificCause().getMessage());
            return List.of();
        }
    }

    public List<String> listarMotoristas() {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        return manifestosRepository.findAll().stream()
                .filter(row -> escopo.permiteAlgumaFilial(row.getFilial()))
                .map(VisaoManifestosEntity::getMotorista)
                .filter(this::temTexto)
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<VeiculoDimDTO> listarVeiculos() {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        Set<String> placasPermitidas = manifestosRepository.findAll().stream()
                .filter(row -> escopo.permiteAlgumaFilial(row.getFilial()))
                .map(VisaoManifestosEntity::getVeiculoPlaca)
                .filter(this::temTexto)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return dimVeiculoRepository.findAll().stream()
                .filter(veiculo -> placasPermitidas.contains(veiculo.getPlaca()))
                .map(this::mapearVeiculo)
                .sorted((a, b) -> a.placa().compareToIgnoreCase(b.placa()))
                .toList();
    }

    public List<PlanoContasDimDTO> listarPlanoContas() {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        return contasAPagarRepository.findAll().stream()
                .filter(row -> escopo.permiteAlgumaFilial(row.getFilial()))
                .filter(row -> temTexto(row.getClassificacaoContabil()) || temTexto(row.getDescricaoContabil()))
                .map(row -> new PlanoContasDimDTO(
                        row.getDescricaoContabil() != null ? row.getDescricaoContabil().trim() : "",
                        row.getClassificacaoContabil() != null ? row.getClassificacaoContabil().trim() : ""
                ))
                .distinct()
                .sorted((a, b) -> a.descricao().compareToIgnoreCase(b.descricao()))
                .toList();
    }

    public List<UsuarioDimDTO> listarUsuarios() {
        EscopoFilialService.EscopoFilial escopo = escopoFilialService.escopoAtual();
        Set<String> nomesPermitidos = coletasRepository.findAll().stream()
                .filter(row -> escopo.permiteAlgumaFilial(row.getFilialNome()))
                .map(row -> row.getUsuarioNome())
                .filter(this::temTexto)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return dimUsuarioRepository.findAll().stream()
                .filter(usuario -> nomesPermitidos.contains(usuario.getNome()))
                .map(this::mapearUsuario)
                .sorted((a, b) -> a.nome().compareToIgnoreCase(b.nome()))
                .toList();
    }

    private VeiculoDimDTO mapearVeiculo(DimVeiculoEntity veiculo) {
        return new VeiculoDimDTO(veiculo.getPlaca(), veiculo.getTipoVeiculo(), veiculo.getProprietario());
    }

    private UsuarioDimDTO mapearUsuario(DimUsuarioEntity usuario) {
        return new UsuarioDimDTO(usuario.getUserId(), usuario.getNome());
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private Stream<String> textoStream(String... valores) {
        return Stream.of(valores)
                .filter(this::temTexto)
                .map(String::trim);
    }
}
