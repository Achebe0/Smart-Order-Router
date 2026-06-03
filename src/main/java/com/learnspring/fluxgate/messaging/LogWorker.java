package com.learnspring.fluxgate.messaging;

import com.learnspring.fluxgate.config.RabbitConfig;
import com.learnspring.fluxgate.dto.PromptLogDTO;
import com.learnspring.fluxgate.model.PromptLog;
import com.learnspring.fluxgate.repository.PromptLogRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LogWorker {

    private final PromptLogRepository repository;

    public LogWorker(PromptLogRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void processLog(PromptLogDTO dto) {
        System.out.println("Received log from RabbitMQ. Saving to DB...");
        
        PromptLog log = new PromptLog(
            dto.originalTokens(),
            dto.optimizedTokens(),
            dto.originalPrompt(),
            dto.optimizedPrompt(),
            dto.selectedModel()
        );
        
        repository.save(log);
        System.out.println(" Log saved successfully.");
    }
}