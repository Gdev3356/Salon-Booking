package br.com.fiap.agendamentocomia.seeder;

import br.com.fiap.agendamentocomia.model.Appointment;
import br.com.fiap.agendamentocomia.model.AppointmentStatus;
import br.com.fiap.agendamentocomia.repository.AppointmentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseSeeder {

    private final AppointmentRepository appointmentRepository;

    public DatabaseSeeder(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @PostConstruct
    public void init() {
        if (appointmentRepository.count() == 0) {
            appointmentRepository.saveAll(List.of(
                    new Appointment(null, "Jose Silva", "Corte de Cabelo", "02/11/2025", "10:00", AppointmentStatus.COMPLETED),
                    new Appointment(null, "Maria Oliveira", "Manicure", "03/11/2025", "14:00", AppointmentStatus.SCHEDULED),
                    new Appointment(null, "Carlos Souza", "Massagem", "04/11/2025", "16:00", AppointmentStatus.CANCELED)
            ));
        }
    }

}
