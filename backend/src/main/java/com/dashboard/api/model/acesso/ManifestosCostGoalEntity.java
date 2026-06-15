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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "manifestos_cost_goals", schema = "acesso")
public class ManifestosCostGoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", length = 120)
    private String branchId;

    @Column(name = "year_month", nullable = false)
    private LocalDate yearMonth;

    @Column(name = "cost_goal", nullable = false, precision = 18, scale = 2)
    private BigDecimal costGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private UsuarioEntity updatedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        normalizarCompetencia();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        normalizarCompetencia();
        updatedAt = Instant.now();
    }

    private void normalizarCompetencia() {
        if (yearMonth != null) {
            yearMonth = yearMonth.withDayOfMonth(1);
        }
    }

    public Long getId() {
        return id;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public LocalDate getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(LocalDate yearMonth) {
        this.yearMonth = yearMonth;
    }

    public BigDecimal getCostGoal() {
        return costGoal;
    }

    public void setCostGoal(BigDecimal costGoal) {
        this.costGoal = costGoal;
    }

    public UsuarioEntity getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(UsuarioEntity updatedByUser) {
        this.updatedByUser = updatedByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
