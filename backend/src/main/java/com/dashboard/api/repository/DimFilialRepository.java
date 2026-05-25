package com.dashboard.api.repository;

import com.dashboard.api.model.DimFilialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DimFilialRepository extends JpaRepository<DimFilialEntity, String> {

    @Query(value = """
            SELECT DISTINCT NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [NomeFilial]))), '') AS nome
            FROM dbo.vw_dim_filiais
            WHERE NULLIF(LTRIM(RTRIM(CONVERT(NVARCHAR(255), [NomeFilial]))), '') IS NOT NULL
            ORDER BY nome
            """, nativeQuery = true)
    List<String> findDistinctNomes();
}
