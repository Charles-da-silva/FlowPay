package com.flowpay.atendimento.application.mapper;

import com.flowpay.atendimento.application.dto.ServiceRequestResponse;
import com.flowpay.atendimento.domain.entity.ServiceRequest;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestMapper {

    public ServiceRequestResponse toResponse(ServiceRequest serviceRequest) {
        Long attendantId = serviceRequest.getAttendant() != null ? serviceRequest.getAttendant().getId() : null;
        String attendantName = serviceRequest.getAttendant() != null ? serviceRequest.getAttendant().getName() : null;
        return new ServiceRequestResponse(
                serviceRequest.getId(),
                serviceRequest.getCustomerName(),
                serviceRequest.getCategory(),
                serviceRequest.getStatus(),
                attendantId,
                attendantName,
                serviceRequest.getCreatedAt(),
                serviceRequest.getStartedAt(),
                serviceRequest.getFinishedAt(),
                serviceRequest.getQueuedAt()
        );
    }
}
