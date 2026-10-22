package br.com.fiap.agendamentocomia.views;

import br.com.fiap.agendamentocomia.model.Appointment;
import br.com.fiap.agendamentocomia.repository.AppointmentRepository;
import br.com.fiap.agendamentocomia.service.AppointmentService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("agenda")
public class Calendar extends VerticalLayout {

    public Calendar(AppointmentService appointmentService) {

        Grid<Appointment> grid = new Grid<>(Appointment.class, false);
        grid.addColumn(Appointment::getId).setHeader("Id");
        grid.addColumn(Appointment::getClientName).setHeader("Nome");
        grid.addColumn(Appointment::getService).setHeader("Serviço");
        grid.addColumn(Appointment::getAppointmentDate).setHeader("Data");
        grid.addColumn(Appointment::getAppointmentTime).setHeader("Hora");
        grid.addColumn(createStatusComponentRenderer()).setHeader("Status");
        grid.setSizeFull();

        setSizeFull();

        List<Appointment> people = appointmentService.getAppointments();
        grid.setItems(people);

        add(new H2("Agenda"));
        add(grid);
    }

    private static final SerializableBiConsumer<Span, Appointment> statusComponentUpdater = (
            span, appointment) -> {

        String theme = switch (appointment.getStatus()) {
            case COMPLETED -> "badge success";
            case SCHEDULED -> "badge contrast";
            case CANCELED -> "badge error";
        };
        span.getElement().setAttribute("theme", theme);
        span.setText(appointment.getStatus().name());
    };

    private static ComponentRenderer<Span, Appointment> createStatusComponentRenderer() {
        return new ComponentRenderer<>(Span::new, statusComponentUpdater);
    }
}
