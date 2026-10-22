package br.com.fiap.agendamentocomia.service;

import br.com.fiap.agendamentocomia.tools.AppointmentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder, VectorStore vectorStore, AppointmentTool appointmentTool) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();

        this.chatClient = builder
                .defaultSystem("""
                        Você é um assistente virtual especializado em agendamentos de um salão de beleza.
                        Responda sempre em português, de forma curta e cordial.
                        Use as regras do salão fornecidas no contexto para informar serviços, valores,
                        horários de funcionamento, cancelamento e remarcação.
                        NÃO faça cálculos de data, hora ou antecedência por conta própria: quando tiver os dados
                        necessários, chame a ferramenta correspondente e repasse ao cliente o resultado dela.
                        Use getCurrentDateTime apenas para saber qual é a data de hoje ao interpretar
                        expressões como "hoje" e "amanhã".
                        Para agendar você precisa de: nome completo, serviço, data e hora.
                        Para cancelar ou remarcar, peça o ID do agendamento.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(4).build())
                                .build())
                .defaultTools(appointmentTool)
                .build();
    }

    public String sendMessage(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}