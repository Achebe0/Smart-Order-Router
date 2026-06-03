package com.learnspring.fluxgate.LlmClients;

import com.learnspring.fluxgate.dto.ChatRequest;
import com.learnspring.fluxgate.dto.ChatResponse;
import com.learnspring.fluxgate.dto.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class NvidiaClient implements LlmProvider {

    private final WebClient webClient;
    private final boolean enabled;
    // OFFICIAL NVIDIA API: Llama 3.1 8B (Small & Fast)
    private final String modelName = "meta/llama-3.1-8b-instruct";

    public NvidiaClient(
            @Value("${NVIDIA_API_KEY:}") String apiKey, // default empty string
            WebClient.Builder builder
    ) {
        this.enabled = !apiKey.isEmpty();

        if (enabled) {
            this.webClient = builder
                    .baseUrl("https://integrate.api.nvidia.com/v1")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        } else {
            this.webClient = null; // won’t be used
            System.out.println("Warning: Nvidia API key not set. ChimeraClient disabled.");
        }
    }

    @Override
    public String getName() {
        return "Nvidia Llama 3.1 8B";
    }

    @Override
    public String generate(String prompt) {
        ChatRequest request = new ChatRequest(
              modelName,
              List.of(new Message("user", prompt)),
                0.3,
                1024,
                false
        );

        long startTime = System.nanoTime(); // Start timing

        try {
            ChatResponse response = webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            long endTime = System.nanoTime(); // End timing
            double durationInMillis = (endTime - startTime) / 1_000_000.0;
            System.out.println("Nvidia API call latency: " + durationInMillis + " ms"); // Log latency

            if(response != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            }
        } catch(Exception e){
            long endTime = System.nanoTime(); // End timing even on error
            double durationInMillis = (endTime - startTime) / 1_000_000.0;
            System.err.println("Error calling Nvidia API after " + durationInMillis + " ms: "+ e.getMessage());
            return "Error calling Nvidia API: "+ e.getMessage();
        }
        return "No response from Nvidia API";
    }
}
