package com.dashboard.api.service;

import java.io.OutputStream;
import java.util.List;
import com.dashboard.api.client.IntegracaoSateliteClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

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
        return integracaoSateliteClient.buscarIntegracoesClientes(params, escopo, dataInicial, dataFinal);
    }

    public ResponseEntity<String> consultarEvolucaoDiaria(
            String dataInicial,
            String dataFinal,
            String escopo,
            List<String> destinos
    ) {
        return integracaoSateliteClient.buscarEvolucaoDiaria(dataInicial, dataFinal, escopo, destinos);
    }

    public void exportarIntegracoes(
            MultiValueMap<String, String> params,
            String escopo,
            String dataInicial,
            String dataFinal,
            OutputStream outputStream
    ) {
        integracaoSateliteClient.exportarIntegracoesClientes(params, escopo, dataInicial, dataFinal, outputStream);
    }

    public ResponseEntity<String> consultarResumoTabelas(String dataInicial, String dataFinal, List<String> destinos) {
        return integracaoSateliteClient.buscarResumoTabelas(dataInicial, dataFinal, destinos);
    }

    public ResponseEntity<String> consultarImagemCanhoto(Long id) {
        return integracaoSateliteClient.buscarImagemLog(id);
    }

    public ResponseEntity<String> consultarErrosQuarentena(Integer pagina, Integer tamanho, List<String> destinos) {
        return integracaoSateliteClient.buscarErrosQuarentena(pagina, tamanho, destinos);
    }

    public ResponseEntity<String> consultarHistoricoQuarentena(
            Integer pagina, Integer tamanho, String dataInicial, String dataFinal, List<String> destinos
    ) {
        return integracaoSateliteClient.buscarHistoricoQuarentena(
                pagina, tamanho, dataInicial, dataFinal, destinos
        );
    }

    public void exportarHistoricoQuarentena(
            String dataInicial, String dataFinal, List<String> destinos, OutputStream outputStream
    ) {
        integracaoSateliteClient.exportarHistoricoQuarentena(dataInicial, dataFinal, destinos, outputStream);
    }

    public void exportarErrosQuarentena(List<String> destinos, OutputStream outputStream) {
        integracaoSateliteClient.exportarErrosQuarentena(destinos, outputStream);
    }
}
