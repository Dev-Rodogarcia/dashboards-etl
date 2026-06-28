package com.dashboard.api.service;

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

    public ResponseEntity<String> consultarIntegracoes(MultiValueMap<String, String> params, String escopo) {
        return integracaoSateliteClient.buscarIntegracoesClientes(params, escopo);
    }

    public ResponseEntity<String> consultarImagemCanhoto(Long id) {
        return integracaoSateliteClient.buscarImagemLog(id);
    }
}
