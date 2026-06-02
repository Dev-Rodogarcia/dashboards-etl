package com.dashboard.api.repository;

import com.dashboard.api.model.DimVeiculoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DimVeiculoRepository extends JpaRepository<DimVeiculoEntity, String> {

    @Query(value = """
            SELECT DISTINCT
                v.[Placa] AS placa,
                v.[TipoVeiculo] AS tipoVeiculo,
                v.[Proprietario] AS proprietario
            FROM dbo.vw_dim_veiculos v
            JOIN (
                SELECT DISTINCT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [Veículo/Placa]))), '') AS placa
                FROM dbo.vw_manifestos_powerbi
                WHERE NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [Veículo/Placa]))), '') IS NOT NULL
            ) manifestos ON manifestos.placa = v.[Placa]
            ORDER BY v.[Placa]
            """, nativeQuery = true)
    List<VeiculoDimProjection> findVeiculosComManifestos();

    @Query(value = """
            SELECT DISTINCT
                v.[Placa] AS placa,
                v.[TipoVeiculo] AS tipoVeiculo,
                v.[Proprietario] AS proprietario
            FROM dbo.vw_dim_veiculos v
            JOIN (
                SELECT DISTINCT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [Veículo/Placa]))), '') AS placa
                FROM dbo.vw_manifestos_powerbi
                WHERE NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(50), [Veículo/Placa]))), '') IS NOT NULL
                  AND LOWER(NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [Filial]))), '')) IN (:filiais)
            ) manifestos ON manifestos.placa = v.[Placa]
            ORDER BY v.[Placa]
            """, nativeQuery = true)
    List<VeiculoDimProjection> findVeiculosComManifestosByFilialIn(@Param("filiais") List<String> filiais);

    interface VeiculoDimProjection {
        String getPlaca();

        String getTipoVeiculo();

        String getProprietario();
    }
}
