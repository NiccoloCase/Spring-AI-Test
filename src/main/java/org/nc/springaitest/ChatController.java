package org.nc.springaitest;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mistralai.MistralAiChatModel;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class ChatController {

    private final MistralAiChatModel chatModel;

    @Autowired
    public ChatController(MistralAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        try {
            String result = this.chatModel.call(message);
            return ResponseEntity.ok(Map.of("generation", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Something went wrong: " + e.getMessage()));
        }
    }

    @GetMapping("/generateStream")
    public Flux<ChatResponse> generateStream(
            @RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        var prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }
}
