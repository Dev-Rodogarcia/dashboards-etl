package com.dashboard.api.model.acesso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "kpi_goals",
        schema = "acesso",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_kpi_goals_branch_indicator_competencia",
                columnNames = {"branch_id", "indicator_key", "competencia"}
        )
)
public class KpiGoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", length = 120)
    private String branchId;

    @Column(name = "indicator_key", nullable = false, length = 60)
    private String indicatorKey;

    @Column(name = "goal_value", nullable = false, precision = 9, scale = 3)
    private BigDecimal goalValue;

    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private UsuarioEntity updatedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getIndicatorKey() { return indicatorKey; }
    public void setIndicatorKey(String indicatorKey) { this.indicatorKey = indicatorKey; }
    public BigDecimal getGoalValue() { return goalValue; }
    public void setGoalValue(BigDecimal goalValue) { this.goalValue = goalValue; }
    public LocalDate getCompetencia() { return competencia; }
    public void setCompetencia(LocalDate competencia) { this.competencia = competencia; }
    public UsuarioEntity getUpdatedByUser() { return updatedByUser; }
    public void setUpdatedByUser(UsuarioEntity updatedByUser) { this.updatedByUser = updatedByUser; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
