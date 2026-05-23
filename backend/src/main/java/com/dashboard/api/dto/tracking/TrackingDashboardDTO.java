package com.dashboard.api.dto.tracking;

import java.util.List;

public record TrackingDashboardDTO(
        TrackingOverviewDTO overview,
        List<TrackingMatrizRegiaoDTO> matrizRegiaoDestino,
        TrackingChartsDTO graficos
) {
}
