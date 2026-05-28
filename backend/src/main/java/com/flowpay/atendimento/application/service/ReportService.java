package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.AttendantDailySummaryResponse;
import com.flowpay.atendimento.application.dto.CategoryReportResponse;
import com.flowpay.atendimento.application.dto.DailyReportResponse;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.entity.ServiceRequest;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantPauseRepository;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Service
public class ReportService {

    private final AttendantRepository attendantRepository;
    private final AttendantPauseRepository attendantPauseRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public ReportService(
            AttendantRepository attendantRepository,
            AttendantPauseRepository attendantPauseRepository,
            ServiceRequestRepository serviceRequestRepository
    ) {
        this.attendantRepository = attendantRepository;
        this.attendantPauseRepository = attendantPauseRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(LocalDate date) {
        return getPeriodReport(date, date);
    }

    @Transactional(readOnly = true)
    public DailyReportResponse getPeriodReport(LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);

        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<ServiceRequest> requests = serviceRequestRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);

        long total = requests.size();
        long waited = requests.stream().filter(request -> request.getQueuedAt() != null).count();
        long inProgress = countByStatus(requests, ServiceRequestStatus.IN_PROGRESS);
        long completed = countByStatus(requests, ServiceRequestStatus.COMPLETED);
        double serviceLevel = calculateServiceLevel(total, waited);
        double averageServiceSeconds = averageCompletedSeconds(requests);

        return new DailyReportResponse(
                startDate,
                endDate,
                Instant.now(),
                total,
                waited,
                inProgress,
                completed,
                serviceLevel,
                averageServiceSeconds,
                buildCategoryReports(requests),
                buildAttendantReports(start, end)
        );
    }

    public String getDailyReportCsv(LocalDate date) {
        return getPeriodReportCsv(date, date);
    }

    public String getPeriodReportCsv(LocalDate startDate, LocalDate endDate) {
        DailyReportResponse report = getPeriodReport(startDate, endDate);
        StringBuilder csv = new StringBuilder();

        csv.append("Relatorio gerencial FlowPay\n");
        csv.append("Data inicial,").append(report.startDate()).append("\n");
        csv.append("Data final,").append(report.endDate()).append("\n");
        csv.append("Gerado em,").append(report.generatedAt()).append("\n\n");

        csv.append("Resumo\n");
        csv.append("Atendimentos,Entraram em espera,Em atendimento,Concluidos,Service Level,Tempo medio atendimento segundos\n");
        csv.append(report.totalServiceRequests()).append(",")
                .append(report.waitedServiceRequests()).append(",")
                .append(report.inProgressServiceRequests()).append(",")
                .append(report.completedServiceRequests()).append(",")
                .append(formatNumber(report.serviceLevel())).append(",")
                .append(formatNumber(report.averageServiceSeconds())).append("\n\n");

        csv.append("Categorias\n");
        csv.append("Categoria,Atendimentos,Entraram em espera,Em atendimento,Concluidos,Tempo medio atendimento segundos\n");
        report.categories().forEach(category -> csv.append(category.category()).append(",")
                .append(category.totalServiceRequests()).append(",")
                .append(category.waitedServiceRequests()).append(",")
                .append(category.inProgressServiceRequests()).append(",")
                .append(category.completedServiceRequests()).append(",")
                .append(formatNumber(category.averageServiceSeconds())).append("\n"));

        csv.append("\nAgentes\n");
        csv.append("Badge,Agente,Atendimentos,Tempo medio atendimento segundos,Pausas,Tempo total pausa segundos\n");
        report.attendants().forEach(attendant -> csv.append(escapeCsv(attendant.attendantBadge())).append(",")
                .append(escapeCsv(attendant.attendantName())).append(",")
                .append(attendant.serviceRequests()).append(",")
                .append(formatNumber(attendant.averageServiceSeconds())).append(",")
                .append(attendant.pauseCount()).append(",")
                .append(formatNumber(attendant.totalPauseSeconds())).append("\n"));

        return csv.toString();
    }

    private List<CategoryReportResponse> buildCategoryReports(List<ServiceRequest> requests) {
        return Arrays.stream(ServiceCategory.values())
                .map(category -> {
                    List<ServiceRequest> categoryRequests = requests.stream()
                            .filter(request -> request.getCategory() == category)
                            .toList();

                    return new CategoryReportResponse(
                            category,
                            categoryRequests.size(),
                            categoryRequests.stream().filter(request -> request.getQueuedAt() != null).count(),
                            countByStatus(categoryRequests, ServiceRequestStatus.IN_PROGRESS),
                            countByStatus(categoryRequests, ServiceRequestStatus.COMPLETED),
                            averageCompletedSeconds(categoryRequests)
                    );
                })
                .toList();
    }

    private List<AttendantDailySummaryResponse> buildAttendantReports(Instant start, Instant end) {
        return attendantRepository.findAll()
                .stream()
                .map(attendant -> toAttendantReport(attendant, start, end))
                .toList();
    }

    private AttendantDailySummaryResponse toAttendantReport(Attendant attendant, Instant start, Instant end) {
        return new AttendantDailySummaryResponse(
                attendant.getId(),
                attendant.getName(),
                attendant.getBadge(),
                serviceRequestRepository.countByAttendantIdAndCreatedAtBetween(attendant.getId(), start, end),
                serviceRequestRepository.averageCompletedServiceSecondsByAttendantAndCreatedAtBetween(
                        attendant.getId(),
                        start,
                        end
                ),
                attendantPauseRepository.countByAttendantIdAndStartedAtBetween(attendant.getId(), start, end),
                attendantPauseRepository.sumPauseSecondsByAttendantAndStartedAtBetween(attendant.getId(), start, end)
        );
    }

    private long countByStatus(List<ServiceRequest> requests, ServiceRequestStatus status) {
        return requests.stream()
                .filter(request -> request.getStatus() == status)
                .count();
    }

    private double calculateServiceLevel(long total, long waited) {
        if (total == 0) {
            return 100.0;
        }
        return ((total - waited) * 100.0) / total;
    }

    private double averageCompletedSeconds(List<ServiceRequest> requests) {
        return requests.stream()
                .filter(request -> request.getStartedAt() != null && request.getFinishedAt() != null)
                .mapToLong(request -> Duration.between(request.getStartedAt(), request.getFinishedAt()).toSeconds())
                .average()
                .orElse(0.0);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalStateException("Data inicial nao pode ser maior que a data final.");
        }
    }

    private String formatNumber(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
