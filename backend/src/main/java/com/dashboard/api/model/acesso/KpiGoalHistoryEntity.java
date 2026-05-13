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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "kpi_goals_history", schema = "acesso")
public class KpiGoalHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", length = 120)
    private String branchId;

    @Column(name = "indicator_key", nullable = false, length = 60)
    private String indicatorKey;

    @Column(name = "old_value", precision = 9, scale = 3)
    private BigDecimal oldValue;

    @Column(name = "new_value", precision = 9, scale = 3)
    private BigDecimal newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private UsuarioEntity updatedByUser;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @PrePersist
    void prePersist() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getIndicatorKey() { return indicatorKey; }
    public void setIndicatorKey(String indicatorKey) { this.indicatorKey = indicatorKey; }
    public BigDecimal getOldValue() { return oldValue; }
    public void setOldValue(BigDecimal oldValue) { this.oldValue = oldValue; }
    public BigDecimal getNewValue() { return newValue; }
    public void setNewValue(BigDecimal newValue) { this.newValue = newValue; }
    public UsuarioEntity getUpdatedByUser() { return updatedByUser; }
    public void setUpdatedByUser(UsuarioEntity updatedByUser) { this.updatedByUser = updatedByUser; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
