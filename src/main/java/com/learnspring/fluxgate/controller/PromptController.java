package com.learnspring.fluxgate.controller;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.learnspring.fluxgate.dto.OptimizationResponse;
import com.learnspring.fluxgate.dto.PromptLogDTO;
import com.learnspring.fluxgate.messaging.LogProducer;
import com.learnspring.fluxgate.model.PromptLog;
import com.learnspring.fluxgate.repository.PromptLogRepository;
import com.learnspring.fluxgate.service.LlmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/prompt")
@CrossOrigin(origins = "*")
public class PromptController {

    private final LlmService llmService;
    private final PromptLogRepository promptLogRepository;
    private final LogProducer logProducer; // Re-enabled for async logging

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry(); // tokenizers from OpenAI
    private final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);


    private static final Set<String> COMPLEX_KEYWORDS = Set.of(
        "essay", "story", "analyze", "explain", "reason", "compare", "contrast", 
        "define", "summarize", "translate",
        "poem", "lyrics", "create", "generate", "build",
        "code", "script", "program", "debug", "refactor",
        "example", "list"
    );

    @Autowired
    public PromptController(LlmService llmService, 
                            PromptLogRepository promptLogRepository,
                            LogProducer logProducer) { // Re-enabled
        this.llmService = llmService;
        this.promptLogRepository = promptLogRepository;
        this.logProducer = logProducer;
    }

    @PostMapping("/optimize")
    public ResponseEntity<OptimizationResponse> optimizePrompt(@RequestBody String prompt) {

        LlmService.ModelType selectedType;
        String routingReason;
        // this should be a list for better accuracy and potential latency
        int length = prompt.length();
        String lowerPrompt = prompt.toLowerCase();

        boolean isComplex = COMPLEX_KEYWORDS.stream().anyMatch(lowerPrompt::contains);
// I want to make this logic more complex
        if (length > 200 || isComplex) {
            selectedType = LlmService.ModelType.FAST_AND_REASONING;
            routingReason = isComplex 
                ? "Complex intent detected. Routing to Llama 3 8B (Reasoning)." 
                : "Long prompt. Routing to Llama 3 8B (Reasoning).";
        } else if (length >= 50) {
            selectedType = LlmService.ModelType.FAST;
            routingReason = "Medium prompt. Routing to Llama 3 8B (Fast).";
        } else {
            selectedType = LlmService.ModelType.SMALL;
            routingReason = "Short/Simple prompt. Routing to Llama 3 8B (Small).";
        }
        
        String optimizedPrompt = llmService.optimizePrompt(prompt, selectedType);
        String finalAnswer = llmService.generateResponse(optimizedPrompt, selectedType);
        String modelName = llmService.getModelName(selectedType);
        
        int originalTokens = encoding.countTokens(prompt);
        int optimizedTokens = encoding.countTokens(optimizedPrompt);
        
        // ASYNC LOGGING via RabbitMQ
        PromptLogDTO logDTO = new PromptLogDTO(
                originalTokens,
                optimizedTokens,
                prompt,
                optimizedPrompt,
                modelName
        );
        logProducer.sendLog(logDTO);
        // --- Latency Reduction: Asynchronous Logging via RabbitMQ ---

       // Offloads prompt log DB writes to a RabbitMQ worker to reduce blocking and lower p95 latency for optimization
        OptimizationResponse response = new OptimizationResponse(
            prompt,
            optimizedPrompt,
            originalTokens,
            optimizedTokens,
            finalAnswer,
            modelName + " | " + routingReason
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<PromptLog>> getHistory() {
        List<PromptLog> history = promptLogRepository.findAll();
        return ResponseEntity.ok(history);
    }

    private String getMatchedKeyword(String prompt) {
        return COMPLEX_KEYWORDS.stream()
            .filter(prompt::contains)
            .findFirst() // finds the keyword in the list
            .orElse("complex"); // assume its complex if not found
    }
}