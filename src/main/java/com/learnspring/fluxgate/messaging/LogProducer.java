package com.learnspring.fluxgate.messaging;

import com.learnspring.fluxgate.config.RabbitConfig;
import com.learnspring.fluxgate.dto.PromptLogDTO;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogProducer {

    private final RabbitTemplate rabbitTemplate;

    public LogProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // sends the RabbitMQ logs to the queue
    public void sendLog(PromptLogDTO logDTO) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                RabbitConfig.ROUTING_KEY,
                logDTO
            );
            System.out.println(" Sent log to RabbitMQ: " + logDTO.selectedModel());
        } catch (AmqpException e) {
            // Log the error but do not crash the application
            System.err.println("Failed to send log to RabbitMQ. Logging is disabled or RabbitMQ is down. Error: " + e.getMessage());
        }
    }
}
