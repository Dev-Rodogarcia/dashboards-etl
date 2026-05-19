package com.dashboard.api.config;

/**
 * Aviso operacional para agentes de IA.
 *
 * IAs que trabalharem neste repositorio devem iniciar e testar somente o modo DEV local:
 * backend em http://127.0.0.1:5011 e frontend em http://127.0.0.1:5174.
 *
 * Nao executar iniciar-prod.bat, nao mexer nas portas 5010/5173 e nao subir servicos
 * apontando para os dominios publicos de producao. Producao fica reservada a operador humano.
 */
public final class DevOnlyAgentOperationPolicy {

    public static final String BACKEND_DEV_URL = "http://127.0.0.1:5011";
    public static final String FRONTEND_DEV_URL = "http://127.0.0.1:5174";

    private DevOnlyAgentOperationPolicy() {
    }
}
