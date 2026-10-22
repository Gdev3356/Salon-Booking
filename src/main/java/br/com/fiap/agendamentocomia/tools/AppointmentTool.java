package br.com.fiap.agendamentocomia.tools;

import br.com.fiap.agendamentocomia.model.Appointment;
import br.com.fiap.agendamentocomia.model.AppointmentStatus;
import br.com.fiap.agendamentocomia.service.AppointmentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AppointmentTool {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final LocalTime OPENING = LocalTime.of(9, 0);
    private static final LocalTime CLOSING = LocalTime.of(19, 0);

    private static final Map<String, Integer> PRICES = new LinkedHashMap<>();

    static {
        PRICES.put("Corte de cabelo feminino", 80);
        PRICES.put("Corte de cabelo masculino", 50);
        PRICES.put("Escova/chapinha", 120);
        PRICES.put("Coloração", 200);
        PRICES.put("Manicure e pedicure", 30);
        PRICES.put("Design de sobrancelhas", 40);
        PRICES.put("Maquiagem", 180);
        PRICES.put("Tratamentos capilares", 190);
    }

    // Feriados nacionais fixos (os móveis, como Carnaval e Sexta-feira Santa, não estão incluídos)
    private static final Set<MonthDay> HOLIDAYS = Set.of(
            MonthDay.of(1, 1), MonthDay.of(4, 21), MonthDay.of(5, 1), MonthDay.of(9, 7),
            MonthDay.of(10, 12), MonthDay.of(11, 2), MonthDay.of(11, 15), MonthDay.of(11, 20),
            MonthDay.of(12, 25));

    private final AppointmentService appointmentService;

    public AppointmentTool(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Tool(description = "Retorna a data e hora atuais, com o dia da semana. Use para interpretar 'hoje', 'amanhã', etc.")
    public String getCurrentDateTime() {
        return LocalDateTime.now(ZONE)
                .format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")));
    }

    @Tool(description = "Realiza um novo agendamento. A própria ferramenta valida todas as regras do salão (serviço, expediente, antecedência, disponibilidade). Chame assim que tiver nome completo, serviço, data e hora, e repasse ao cliente exatamente o resultado.")
    public String createAppointment(
            @ToolParam(description = "Nome completo do cliente") String clientName,
            @ToolParam(description = "Serviço desejado, como listado nas regras") String service,
            @ToolParam(description = "Data no formato dd/MM/yyyy") String appointmentDate,
            @ToolParam(description = "Hora no formato HH:mm") String appointmentTime) {

        if (clientName == null || clientName.trim().split("\\s+").length < 2) {
            return "Rejeitado: informe o nome completo do cliente (nome e sobrenome).";
        }

        Optional<String> canonicalService = findService(service);
        if (canonicalService.isEmpty()) {
            return "Rejeitado: serviço inválido. Serviços disponíveis: " + String.join(", ", PRICES.keySet()) + ".";
        }

        LocalDateTime dateTime;
        try {
            dateTime = parse(appointmentDate, appointmentTime);
        } catch (DateTimeParseException e) {
            return "Rejeitado: data ou hora em formato inválido. Use dd/MM/yyyy e HH:mm.";
        }

        String error = validateSlot(dateTime);
        if (error != null) {
            return "Rejeitado: " + error;
        }

        Appointment saved = appointmentService.createAppointment(new Appointment(
                null, clientName.trim(), canonicalService.get(),
                dateTime.format(DATE_FMT), dateTime.format(TIME_FMT), AppointmentStatus.SCHEDULED));

        return "Agendamento confirmado! ID: " + saved.getId()
                + " | Cliente: " + saved.getClientName()
                + " | Serviço: " + saved.getService()
                + " | Valor: R$ " + PRICES.get(saved.getService())
                + " | Data: " + saved.getAppointmentDate()
                + " | Hora: " + saved.getAppointmentTime();
    }

    @Tool(description = "Cancela um agendamento pelo ID. A ferramenta aplica a regra de 6 horas e informa se pode haver taxa de 50%.")
    public String cancelAppointment(
            @ToolParam(description = "ID do agendamento") Long appointmentId) {

        Optional<Appointment> found = appointmentService.getAppointment(appointmentId);
        if (found.isEmpty()) {
            return "Agendamento " + appointmentId + " não encontrado.";
        }

        Appointment appointment = found.get();
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            return "Não é possível cancelar: o agendamento " + appointmentId
                    + " está com status " + appointment.getStatus() + ".";
        }

        String message = "Agendamento " + appointmentId + " cancelado com sucesso.";
        try {
            LocalDateTime current = parse(appointment.getAppointmentDate(), appointment.getAppointmentTime());
            if (current.isBefore(LocalDateTime.now(ZONE).plusHours(6))) {
                int fee = PRICES.getOrDefault(appointment.getService(), 0) / 2;
                message += " Como o cancelamento foi feito com menos de 6 horas de antecedência, "
                        + "pode ser cobrada taxa de 50% do valor do serviço (R$ " + fee + ").";
            }
        } catch (DateTimeParseException ignored) {
            // data antiga em formato inesperado: cancela sem calcular taxa
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        appointmentService.updateAppointment(appointment, appointmentId);
        return message;
    }

    @Tool(description = "Remarca um agendamento pelo ID para nova data e hora. A ferramenta valida as mesmas regras de um novo agendamento e a antecedência de 6 horas.")
    public String rescheduleAppointment(
            @ToolParam(description = "ID do agendamento") Long appointmentId,
            @ToolParam(description = "Nova data no formato dd/MM/yyyy") String newDate,
            @ToolParam(description = "Nova hora no formato HH:mm") String newTime) {

        Optional<Appointment> found = appointmentService.getAppointment(appointmentId);
        if (found.isEmpty()) {
            return "Agendamento " + appointmentId + " não encontrado.";
        }

        Appointment appointment = found.get();
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            return "Não é possível remarcar: o agendamento " + appointmentId
                    + " está com status " + appointment.getStatus() + ".";
        }

        try {
            LocalDateTime current = parse(appointment.getAppointmentDate(), appointment.getAppointmentTime());
            if (current.isBefore(LocalDateTime.now(ZONE).plusHours(6))) {
                return "Rejeitado: a remarcação sem custos exige antecedência mínima de 6 horas "
                        + "em relação ao horário atual do agendamento.";
            }
        } catch (DateTimeParseException ignored) {
            // data antiga em formato inesperado: segue sem validar a antecedência
        }

        LocalDateTime newDateTime;
        try {
            newDateTime = parse(newDate, newTime);
        } catch (DateTimeParseException e) {
            return "Rejeitado: data ou hora em formato inválido. Use dd/MM/yyyy e HH:mm.";
        }

        String error = validateSlot(newDateTime);
        if (error != null) {
            return "Rejeitado: " + error;
        }

        appointment.setAppointmentDate(newDateTime.format(DATE_FMT));
        appointment.setAppointmentTime(newDateTime.format(TIME_FMT));
        appointmentService.updateAppointment(appointment, appointmentId);
        return "Agendamento " + appointmentId + " remarcado para "
                + appointment.getAppointmentDate() + " às " + appointment.getAppointmentTime() + ".";
    }

    private LocalDateTime parse(String date, String time) {
        return LocalDateTime.of(LocalDate.parse(date.trim(), DATE_FMT), LocalTime.parse(time.trim(), TIME_FMT));
    }

    private Optional<String> findService(String service) {
        if (service == null) {
            return Optional.empty();
        }
        return PRICES.keySet().stream()
                .filter(s -> s.equalsIgnoreCase(service.trim()))
                .findFirst();
    }

    /** Retorna a mensagem de erro, ou null se o horário for válido. */
    private String validateSlot(LocalDateTime dateTime) {
        if (dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "o salão não atende aos domingos.";
        }
        if (HOLIDAYS.contains(MonthDay.from(dateTime))) {
            return "o salão não atende em feriados.";
        }
        LocalTime time = dateTime.toLocalTime();
        if (time.isBefore(OPENING) || !time.isBefore(CLOSING)) {
            return "horário fora do expediente (segunda a sábado, das 09:00 às 19:00).";
        }
        if (dateTime.isBefore(LocalDateTime.now(ZONE).plusHours(24))) {
            return "o agendamento exige antecedência mínima de 24 horas.";
        }
        if (!appointmentService.isAvailable(dateTime.format(DATE_FMT), dateTime.format(TIME_FMT))) {
            return "horário indisponível, já existe um cliente agendado em "
                    + dateTime.format(DATE_FMT) + " às " + dateTime.format(TIME_FMT) + ".";
        }
        return null;
    }
}