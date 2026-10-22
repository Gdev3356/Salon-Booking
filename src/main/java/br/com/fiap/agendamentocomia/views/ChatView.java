package br.com.fiap.agendamentocomia.views;

import br.com.fiap.agendamentocomia.service.ChatService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.Instant;

@Route("chat")
public class ChatView extends VerticalLayout {
    private final MessageList messageList = new MessageList();
    private final ChatService chatService;

    public ChatView(ChatService chatService) {
        this.chatService = chatService;

        var messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageList.setSizeFull();
        setHeightFull();
        messageInput.addSubmitListener(e -> handleSend(e .getValue()));

        add(new H2("Chat"));
        add(new Paragraph("Como podemos ajudar você hoje?"));
        add(messageList);
        add(messageInput);
    }

    private void handleSend(String message) {
        messageList.addItem(new MessageListItem(
                message, Instant.now(), "Cliente"
        ));

        var response =  chatService.sendMessage(message);
        messageList.addItem(new MessageListItem(
                response, Instant.now(), "Atendente"
        ));
    }
}
