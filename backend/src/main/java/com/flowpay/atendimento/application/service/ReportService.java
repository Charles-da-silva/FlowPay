package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.AttendantDailySummaryResponse;
import com.flowpay.atendimento.application.dto.CategoryReportResponse;
import com.flowpay.atendimento.application.dto.DailyReportResponse;
import com.flowpay.atendimento.application.dto.DailyStatisticsResponse;
import com.flowpay.atendimento.application.dto.WorstServiceLevelDayResponse;
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
                buildAttendantReports(start, end),
                buildWorstDaysReport(requests)
        );
    }

    public String getDailyReportCsv(LocalDate date) {
        return getPeriodReportCsv(date, date);
    }

    public String getPeriodReportCsv(LocalDate startDate, LocalDate endDate) {
        DailyReportResponse report = getPeriodReport(startDate, endDate);
        StringBuilder csv = new StringBuilder();
        long daysDifference = calculateDaysDifference(startDate, endDate);

        csv.append("\uFEFF");
        csv.append("Relatorio gerencial FlowPay\n");
        csv.append("Data inicial,").append(report.startDate()).append("\n");
        csv.append("Data final,").append(report.endDate()).append("\n");
        csv.append("Gerado em,").append(report.generatedAt()).append("\n\n");

        csv.append("Resumo\n");
        csv.append("Atendimentos,Entraram em espera,Service Level,Tempo medio atendimento minutos\n");
        csv.append(report.totalServiceRequests()).append(",")
                .append(report.waitedServiceRequests()).append(",")
                .append(formatNumber(report.serviceLevel())).append(",")
                .append(formatNumber(report.averageServiceSeconds() / 60.0)).append("\n\n");

        csv.append("Categorias\n");
        csv.append("Categoria,Atendimentos,Entraram em espera,Tempo medio atendimento minutos\n");
        report.categories().forEach(category -> csv.append(getCategoryLabel(category.category())).append(",")
                .append(category.totalServiceRequests()).append(",")
                .append(category.waitedServiceRequests()).append(",")
                .append(formatNumber(category.averageServiceSeconds() / 60.0)).append("\n"));

        csv.append("\nAgentes\n");
        csv.append("Badge,Agente,Atendimentos,Tempo medio atendimento minutos,Pausas,Tempo medio pausa minutos por dia\n");
        report.attendants().forEach(attendant -> csv.append(escapeCsv(attendant.attendantBadge())).append(",")
                .append(escapeCsv(attendant.attendantName())).append(",")
                .append(attendant.serviceRequests()).append(",")
                .append(formatNumber(attendant.averageServiceSeconds() / 60.0)).append(",")
                .append(attendant.pauseCount()).append(",")
                .append(formatNumber(attendant.totalPauseSeconds() / 60.0 / daysDifference)).append("\n"));

        csv.append("\nHistorico Diario\n");
        csv.append("Data,Atendimentos,Entraram em espera,Service Level,Tempo medio atendimento minutos\n");

        List<DailyStatisticsResponse> dailyStats = buildDailyStatistics(startDate, endDate);
        dailyStats.forEach(day -> csv.append(day.date()).append(",")
                .append(day.totalServiceRequests()).append(",")
                .append(day.waitedServiceRequests()).append(",")
                .append(formatNumber(day.serviceLevel())).append(",")
                .append(formatNumber(day.averageServiceSeconds() / 60.0)).append("\n"));

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
                            averageCompletedSeconds(categoryRequests),
                            averageWaitSeconds(categoryRequests)
                    );
                })
                .toList();
    }

    private List<AttendantDailySummaryResponse> buildAttendantReports(Instant start, Instant end) {
        return attendantRepository.findAll()
                .stream()
                .map(attendant -> toAttendantReport(attendant, start, end))
                .filter(report -> report.serviceRequests() > 0 || report.pauseCount() > 0)
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

    private double averageWaitSeconds(List<ServiceRequest> requests) {
        return requests.stream()
                .filter(request -> request.getQueuedAt() != null && request.getStartedAt() != null)
                .mapToLong(request -> Duration.between(request.getQueuedAt(), request.getStartedAt()).toSeconds())
                .average()
                .orElse(0.0);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalStateException("Data inicial nao pode ser maior que a data final.");
        }
        if (endDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("A data final nao pode ser uma data futura.");
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

    private String getCategoryLabel(ServiceCategory category) {
        return switch (category) {
            case CARD_ISSUES -> "Problemas com cartão";
            case LOAN_CONTRACTING -> "Contratação de empréstimo";
            case OTHER_SUBJECTS -> "Outros assuntos";
        };
    }

    private long calculateDaysDifference(LocalDate startDate, LocalDate endDate) {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private List<DailyStatisticsResponse> buildDailyStatistics(LocalDate startDate, LocalDate endDate) {
        List<DailyStatisticsResponse> dailyStats = new java.util.ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            Instant dayStart = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = currentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            List<ServiceRequest> dayRequests = serviceRequestRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(dayStart, dayEnd);

            long dayTotal = dayRequests.size();
            long dayWaited = dayRequests.stream().filter(r -> r.getQueuedAt() != null).count();
            double dayServiceLevel = calculateServiceLevel(dayTotal, dayWaited);
            double dayAverage = averageCompletedSeconds(dayRequests);

            dailyStats.add(new DailyStatisticsResponse(currentDate, dayTotal, dayWaited, dayServiceLevel, dayAverage));
            currentDate = currentDate.plusDays(1);
        }

        return dailyStats;
    }

    private List<WorstServiceLevelDayResponse> buildWorstDaysReport(List<ServiceRequest> requests) {
        return requests.stream()
                .filter(request -> request.getCreatedAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        request -> request.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                ))
                .entrySet().stream()
                .map(entry -> {
                    List<ServiceRequest> dayRequests = entry.getValue();
                    long dayTotal = dayRequests.size();
                    long dayWaited = dayRequests.stream().filter(r -> r.getQueuedAt() != null).count();
                    double dayServiceLevel = calculateServiceLevel(dayTotal, dayWaited);
                    return new WorstServiceLevelDayResponse(entry.getKey(), dayServiceLevel);
                })
                .sorted((a, b) -> Double.compare(a.serviceLevel(), b.serviceLevel()))
                .limit(3)
                .toList();
    }
}
