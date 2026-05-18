package com.odontoflow.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clinics")
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nomeFantasia;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false, length = 500)
    private String endereco;

    @Column(nullable = false)
    private String email;

    private String website;

    private String logoUrl;

    @Column(nullable = false)
    private String duracaoConsulta = "30 min";

    @Column(nullable = false)
    private String intervalo = "15 min";

    @Column(nullable = false, precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 50000.00")
    private BigDecimal revenueGoal = new BigDecimal("50000.00");

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 80")
    private Long treatmentGoal = 80L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "clinic",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("position ASC")
    @BatchSize(size = 7)
    private List<ClinicHour> hours = new ArrayList<>();
}
