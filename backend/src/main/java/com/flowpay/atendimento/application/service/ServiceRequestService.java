package com.flowpay.atendimento.application.service;

import com.flowpay.atendimento.application.dto.CreateServiceRequestRequest;
import com.flowpay.atendimento.application.dto.ServiceRequestResponse;
import com.flowpay.atendimento.application.mapper.ServiceRequestMapper;
import com.flowpay.atendimento.domain.entity.Attendant;
import com.flowpay.atendimento.domain.entity.ServiceRequest;
import com.flowpay.atendimento.domain.enums.ServiceCategory;
import com.flowpay.atendimento.domain.enums.ServiceRequestStatus;
import com.flowpay.atendimento.domain.repository.AttendantRepository;
import com.flowpay.atendimento.domain.repository.ServiceRequestRepository;
import com.flowpay.atendimento.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

@Service
public class ServiceRequestService {

    private final AttendantRepository attendantRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final AttendantService attendantService;
    private final ServiceRequestMapper serviceRequestMapper;
    private final DashboardSseService dashboardSseService;

    public ServiceRequestService(
            AttendantRepository attendantRepository,
            ServiceRequestRepository serviceRequestRepository,
            AttendantService attendantService,
            ServiceRequestMapper serviceRequestMapper,
            DashboardSseService dashboardSseService
    ) {
        this.attendantRepository = attendantRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.attendantService = attendantService;
        this.serviceRequestMapper = serviceRequestMapper;
        this.dashboardSseService = dashboardSseService;
    }

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequestRequest request) {
        ServiceRequest serviceRequest = ServiceRequest.builder()
                .customerName(request.customerName())
                .category(request.category())
                .createdAt(Instant.now())
                .build();

        // Regra principal: tenta alocar imediatamente; sem slot, entra em fila.
        Optional<Attendant> eligible = findEligibleAttendant(request.category());
        ServiceRequest saved;
        if (eligible.isPresent()) {
            Attendant attendant = eligible.get();
            assignToAttendant(serviceRequest, attendant);
            saved = serviceRequestRepository.save(serviceRequest);
            refreshAttendantStatus(attendant);
        } else {
            serviceRequest.setStatus(ServiceRequestStatus.WAITING);
            serviceRequest.setAttendant(null);
            serviceRequest.setQueuedAt(Instant.now());
            saved = serviceRequestRepository.save(serviceRequest);
        }

        notifyDashboardUpdate();
        return serviceRequestMapper.toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse finish(Long serviceRequestId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Atendimento nao encontrado: " + serviceRequestId));

        if (serviceRequest.getStatus() == ServiceRequestStatus.COMPLETED) {
            return serviceRequestMapper.toResponse(serviceRequest);
        }

        Attendant previousAttendant = serviceRequest.getAttendant();

        serviceRequest.setStatus(ServiceRequestStatus.COMPLETED);
        serviceRequest.setFinishedAt(Instant.now());
        serviceRequestRepository.save(serviceRequest);

        if (previousAttendant != null) {
            long openCount = serviceRequestRepository.countByAttendantIdAndStatus(
                    previousAttendant.getId(),
                    ServiceRequestStatus.IN_PROGRESS
            );
            attendantService.refreshStatus(previousAttendant, openCount);
        }

        // Redistribuicao automatica: sempre que libera slot, tenta puxar da fila.
        redistributeQueue(serviceRequest.getCategory());
        notifyDashboardUpdate();

        /* Pega a entidade serviceRequest (com seus novos dados de finalizada), passa pelo Mapper que 
        a transforma em um DTO limpo e envia no retorno do método.*/
        return serviceRequestMapper.toResponse(serviceRequest);
    }


    /* @Transactional(readOnly = true) – Diz ao Spring que este método apenas lê dados do banco, 
    não faz alterações (nem Insert, nem Update, nem Delete). Isso faz com que o Hibernate desative 
    a checagem de modificações (dirty checking), deixando a consulta muito mais leve e performática.
    
    >> serviceRequestRepository.findAll() – Busca todos os registros da tabela de chamados no banco e 
    retorna uma List<ServiceRequest>.
    >> .stream() – Abre um fluxo de dados (Stream API do Java). É como colocar a lista em uma esteira 
    rolante para processar cada item um por um de forma funcional.
    >> .map(serviceRequestMapper::toResponse) – O método map transforma os itens da esteira. Usando a 
    sintaxe de Method Reference (::), ele passa cada entidade da esteira para dentro do método toResponse 
    do Mapper, transformando a lista de entidades em uma lista de DTOs.
    >> .toList() – Fecha a esteira rolante e agrupa todos os elementos transformados de volta em uma 
    nova lista (List), que é retornada pelo método.*/
    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> list() {
        return serviceRequestRepository.findAll()
                .stream()
                .map(serviceRequestMapper::toResponse)
                .toList();
    }

    /* findByStatusOrderByCreatedAtAsc(...) – Uma das mágicas do Spring Data JPA (chamada Derived Query). 
    O Spring lê o nome desse método e cria automaticamente uma consulta SQL com a cláusula 
    WHERE status = 'WAITING' ORDER BY created_at ASC. Isso traz apenas os chamados na fila, 
    priorizando os mais antigos.*/
    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> listQueue() {
        return serviceRequestRepository.findByStatusOrderByCreatedAtAsc(ServiceRequestStatus.WAITING)
                .stream()
                .map(serviceRequestMapper::toResponse)
                .toList();
    }

    private Optional<Attendant> findEligibleAttendant(ServiceCategory category) {

        /* LocalDate.now().atStartOfDay(...) – Captura a data de hoje e define o horário para 00:00:00 
        no fuso horário do sistema, convertendo para o tipo Instant (UTC). endOfDay faz o mesmo, mas 
        soma 1 dia (00:00:00 de amanhã). Isso cria uma janela de tempo para sabermos o que aconteceu 
        "no dia de hoje".*/
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        /* findEligible(category) - busca apenas atendentes que podem atender a categoria
        .stream() - Transforma a lista de atendentes em uma esteira para aplicar ordenação personalizada
        .min(Comparator - Percorre a esteira e pega o "menor" atendente baseado nos critérios que vêm depois:
        
            Critério 1 – Verifica se o atendente tem zero chamados ativos no momento (activeRequestsCount == 0). 
            Se tiver zero, recebe peso 0 (menor). Se tiver chamados, recebe peso 1. Como o método busca o .min(), 
            quem tem peso 0 ganha prioridade máxima.

            Critério 2 – Se houver empate no critério 1 (ex: dois atendentes estão com zero chamados), o 
            thenComparing entra em ação. Ele olha o campo getAvailableSince (disponível desde). Datas mais 
            antigas são consideradas "menores" em Java, logo, quem está esperando há mais tempo na fila de 
            ociosidade ganha a vez. Se não aplicar, joga para o início dos tempos (Instant.EPOCH).

            Critério 3 – Se o empate persistir, ele conta quantos chamados aquele atendente já criou/atendeu
            entre o início e o fim do dia de hoje (countByAttendantIdAndCreatedAtBetween...). Quem tiver o 
            menor número (menos chamados atendidos hoje) ganha a vaga. Isso garante uma distribuição justa 
            de carga de trabalho durante o dia.

            Critério 4 – Se ainda assim empatar, ele olha a quantidade bruta de chamados simultâneos ativos 
            (IN_PROGRESS) que o atendente possui agora. O que tiver menos chamados ganha.

            Critério 5 – O último desempate absoluto: se tudo for idêntico, compara pelo ID do atendente no 
            banco de dados. O menor ID ganha. O resultado de toda essa busca (que pode ser um atendente ou 
            um vazio se a lista inicial do banco veio vazia) é retornado como um Optional<Attendant>
            */
        return attendantRepository.findEligible(category) 
                .stream() 
                .min(Comparator  
                        .comparingLong((Attendant attendant) -> activeRequestsCount(attendant.getId()) == 0 ? 0 : 1)
                        .thenComparing(
                                attendant -> activeRequestsCount(attendant.getId()) == 0 && attendant.getAvailableSince() != null
                                        ? attendant.getAvailableSince()
                                        : Instant.EPOCH // Se o atendente não estiver disponível, atribui a data mais antiga possível para que ele fique por último na ordenação
                        )
                        .thenComparingLong(attendant ->
                                serviceRequestRepository.countByAttendantIdAndCreatedAtBetween( // Atendentes com menos atendimentos no dia têm prioridade
                                        attendant.getId(),
                                        startOfDay,
                                        endOfDay
                                )
                        )
                        .thenComparingLong(attendant -> activeRequestsCount(attendant.getId())) // Atendentes com menos atendimentos em progresso têm prioridade
                        .thenComparing(Attendant::getId)); // Critério final para desempate: ID do atendente (garante ordem consistente)
    }

    private long activeRequestsCount(Long attendantId) {
        return serviceRequestRepository.countByAttendantIdAndStatus(
                attendantId,
                ServiceRequestStatus.IN_PROGRESS
        );
    }

    private void assignToAttendant(ServiceRequest serviceRequest, Attendant attendant) {
        serviceRequest.setAttendant(attendant);
        serviceRequest.setStatus(ServiceRequestStatus.IN_PROGRESS);
        serviceRequest.setStartedAt(Instant.now());
    }

    // Mantem o status do agente coerente com a quantidade real de atendimentos em aberto.
    private void refreshAttendantStatus(Attendant attendant) {
        long openCount = serviceRequestRepository.countByAttendantIdAndStatus(
                attendant.getId(),
                ServiceRequestStatus.IN_PROGRESS
        );
        attendantService.refreshStatus(attendant, openCount);
    }

    private void redistributeQueue(ServiceCategory category) {
        /* Este método é chamado sempre que um atendimento é finalizado ou um agente fica disponível, 
        para tentar puxar o próximo atendimento da fila. */

        /*Este Optional pode ou não retornar um atendente elegível. Se não houver nenhum atendente 
        disponível para a categoria, o método simplesmente retorna sem fazer nada e a variável "eligible" 
        fica vazia (empty).*/
        Optional<Attendant> eligible = findEligibleAttendant(category);

        // Se não houver atendente elegível, não há como redistribuir a fila, então o método retorna sem fazer nada.
        if (eligible.isEmpty()) {
            return;
        }

        /* Este Optional pode ou não retornar um atendimento na fila de espera para a categoria. 
        Se não houver nenhum atendimento esperando, o método retorna sem fazer nada e a variável 
        "nextInQueue" fica vazia (empty).*/
        Optional<ServiceRequest> nextInQueue = serviceRequestRepository.findFirstByStatusAndCategoryOrderByCreatedAtAsc(
                ServiceRequestStatus.WAITING,
                category
        );

        if (nextInQueue.isEmpty()) {
            return;
        }

        /* Caso haja um atendente elegível e um atendimento na fila, o método prossegue para atribuir o 
        atendimento ao atendente.*/
        ServiceRequest next = nextInQueue.get();
        Attendant attendant = eligible.get();
        assignToAttendant(next, attendant);
        serviceRequestRepository.save(next);
        refreshAttendantStatus(attendant);

        // Mantem FIFO enquanto houver slot + fila para a categoria.
        redistributeQueue(category);
    }

    // Dispara evento SSE para todos os dashboards conectados.
    private void notifyDashboardUpdate() {
        dashboardSseService.notifyDashboardChanged();
    }
}
