package com.dashboard.api.service;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Supplier;
import com.dashboard.api.client.IntegracaoSateliteClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class IntegracoesService {

    private final IntegracaoSateliteClient integracaoSateliteClient;

    public IntegracoesService(IntegracaoSateliteClient integracaoSateliteClient) {
        this.integracaoSateliteClient = integracaoSateliteClient;
    }

    public ResponseEntity<String> consultarIntegracoes(
            MultiValueMap<String, String> params,
            String escopo,
            String dataInicial,
            String dataFinal
    ) {
        return consultarSatelite(() -> integracaoSateliteClient.buscarIntegracoesClientes(params, escopo, dataInicial, dataFinal));
    }

    public ResponseEntity<String> consultarEvolucaoDiaria(
            String dataInicial,
            String dataFinal,
            String escopo,
            List<String> destinos
    ) {
        return consultarSatelite(() -> integracaoSateliteClient.buscarEvolucaoDiaria(dataInicial, dataFinal, escopo, destinos));
    }

    public void exportarIntegracoes(
            MultiValueMap<String, String> params,
            String escopo,
            String dataInicial,
            String dataFinal,
            OutputStream outputStream
    ) {
        executarNoSatelite(() -> integracaoSateliteClient.exportarIntegracoesClientes(params, escopo, dataInicial, dataFinal, outputStream));
    }

    public ResponseEntity<String> consultarResumoTabelas(String dataInicial, String dataFinal, List<String> destinos) {
        return consultarSatelite(() -> integracaoSateliteClient.buscarResumoTabelas(dataInicial, dataFinal, destinos));
    }

    public ResponseEntity<String> consultarImagemCanhoto(Long id) {
        return consultarSatelite(() -> integracaoSateliteClient.buscarImagemLog(id));
    }

    public ResponseEntity<String> consultarErrosQuarentena(Integer pagina, Integer tamanho, List<String> destinos) {
        return consultarSatelite(() -> integracaoSateliteClient.buscarErrosQuarentena(pagina, tamanho, destinos));
    }

    public ResponseEntity<String> consultarHistoricoQuarentena(
            Integer pagina, Integer tamanho, String dataInicial, String dataFinal, List<String> destinos
    ) {
        return consultarSatelite(() -> integracaoSateliteClient.buscarHistoricoQuarentena(
                pagina, tamanho, dataInicial, dataFinal, destinos
        ));
    }

    public void exportarHistoricoQuarentena(
            String dataInicial, String dataFinal, List<String> destinos, OutputStream outputStream
    ) {
        executarNoSatelite(() -> integracaoSateliteClient.exportarHistoricoQuarentena(dataInicial, dataFinal, destinos, outputStream));
    }

    public void exportarErrosQuarentena(List<String> destinos, OutputStream outputStream) {
        executarNoSatelite(() -> integracaoSateliteClient.exportarErrosQuarentena(destinos, outputStream));
    }

    private <T> T consultarSatelite(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (ResourceAccessException ex) {
            throw sateliteIndisponivel(ex);
        }
    }

    private void executarNoSatelite(Runnable chamada) {
        try {
            chamada.run();
        } catch (ResourceAccessException ex) {
            throw sateliteIndisponivel(ex);
        }
    }

    private ResponseStatusException sateliteIndisponivel(ResourceAccessException causa) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Satélite de integrações indisponível.", causa);
    }
}
