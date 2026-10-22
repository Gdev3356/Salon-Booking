package br.com.fiap.agendamentocomia.views;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class HomeView extends VerticalLayout {
    public HomeView() {
        add(new Paragraph("Bem-vindo ao Agendamento com I.A.!"));
    }
}

