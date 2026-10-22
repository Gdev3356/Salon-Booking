package br.com.fiap.agendamentocomia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String clientName;
    private String service;
    private String appointmentDate;
    private String appointmentTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

}
