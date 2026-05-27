package com.flowpay.atendimento.domain.entity;

import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ServiceCategory category;

    // Status do atendimento para controlar fila e ciclo de vida.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceRequestStatus status;

    // N:1 -> varios chamados podem estar alocados ao mesmo atendente.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendant_id")
    private Attendant attendant;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
