package com.dashboard.api.util;

public final class ConsultaLimiteUtils {

    private ConsultaLimiteUtils() {
    }

    public static int limitar(int limiteSolicitado, int limitePadrao, int limiteMaximo) {
        if (limiteSolicitado <= 0) {
            return limitePadrao;
        }
        return Math.min(limiteSolicitado, limiteMaximo);
    }
}
