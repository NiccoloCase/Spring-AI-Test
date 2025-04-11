package org.nc.springaitest;
import org.nc.springaitest.dto.IELTSEvaluation;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ai.mistralai.MistralAiChatModel;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class ChatController {

    private final MistralAiChatModel chatModel;

    @Autowired
    public ChatController(MistralAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/scoreEssay")
    public ResponseEntity<IELTSEvaluation> scoreEssay(@RequestBody Map<String, String> request) {
        try {
            String essay = request.getOrDefault("essay", "");

            String prompt = """
                You are an IELTS examiner. Score the following IELTS Writing Task 2 essay.
                Provide the scores as integers (0-9) in the following JSON format:
                {
                    \"taskResponse\": <int>,
                    \"coherenceCohesion\": <int>,
                    \"lexicalResource\": <int>,
                    \"grammaticalRangeAccuracy\": <int>,
                    \"overall\": <int>,
                    \"comment\": \"<short comment about main errors and suggestions to improve>\"
                }

                Essay:
                %s
                """.formatted(essay);

            String result = this.chatModel.call(prompt);
            IELTSEvaluation evaluation = IELTSEvaluation.fromJson(result);
            return ResponseEntity.ok(evaluation);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}


