package com.flowpay.atendimento.domain.entity;

import com.flowpay.atendimento.domain.enums.AttendantStatus;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "attendants")
public class Attendant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendantStatus status;

    // Limite de atendimentos simultaneos por agente.
    @Column(name = "max_simultaneous_customers", nullable = false)
    private Integer maxSimultaneousCustomers = 3;

    @Column(name = "available_since")
    private Instant availableSince;

    @Column(name = "paused_since")
    private Instant pausedSince;

    // Categorias que o agente pode atender.
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attendant_categories", joinColumns = @JoinColumn(name = "attendant_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private Set<ServiceCategory> categories = new HashSet<>();

    // Relacionamento 1:N com os atendimentos (lado inverso do ManyToOne).
    @Builder.Default
    @OneToMany(mappedBy = "attendant")
    private List<ServiceRequest> serviceRequests = new ArrayList<>();
}
