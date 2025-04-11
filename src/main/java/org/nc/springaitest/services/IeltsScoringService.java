package org.nc.springaitest.services;

import org.nc.springaitest.dto.EssayRequest;
import org.nc.springaitest.dto.EvaluationResponse;
import org.nc.springaitest.model.EssayDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IeltsScoringService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private final MistralAiChatModel chatModel;

    @Autowired
    private EvaluationMetrics metrics;

    @Autowired
    private EssayPreprocessor preprocessor;


    @Autowired
    public IeltsScoringService(MistralAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public EvaluationResponse scoreEssay(EssayRequest request) {
        String cleanedEssay = preprocessor.cleanEssay(request.essay());
        String searchQuery = request.question() + "\n" + cleanedEssay;

        // Retrieve similar essays - fixed SearchRequest creation
        List<Document> similarEssays = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchQuery)
                        .topK(5)
                        .similarityThreshold(0.7)
                        .build()
        );

        // Build prompt
        String prompt = buildScoringPrompt(request, cleanedEssay, similarEssays);

        // Get AI evaluation
        String aiResponse = chatModel.call(prompt);

        // Parse response
        EvaluationResponse response = parseAiResponse(aiResponse);

        // Track metrics
        trackEvaluationMetrics(response);

        return response;
    }

    private String buildScoringPrompt(EssayRequest request, String essay, List<Document> examples) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an experienced IELTS examiner. Evaluate this essay based on IELTS Writing Task 2 criteria.\n\n");
        prompt.append("Question: ").append(request.question()).append("\n\n");
        prompt.append("Essay to evaluate:\n").append(essay).append("\n\n");

        prompt.append("Scoring Criteria:\n");
        prompt.append("1. Task Response (TR): Address all parts, develop position, support ideas\n");
        prompt.append("2. Coherence & Cohesion (CC): Logical organization, paragraphing, linking devices\n");
        prompt.append("3. Lexical Resource (LR): Vocabulary range, accuracy, collocations\n");
        prompt.append("4. Grammatical Range & Accuracy (GRA): Sentence structures, grammar, punctuation\n\n");

        if (!examples.isEmpty()) {
            prompt.append("Example Essays for Reference:\n");
            examples.forEach(doc -> {
                if (doc instanceof EssayDocument essayDoc) {
                    prompt.append("--- Band ").append(essayDoc.getBandScore())
                            .append(" Example ---\n")
                            .append(essayDoc.getContent()).append("\n\n");
                }
            });
        }

        prompt.append("Provide evaluation in this exact JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"taskResponse\": [score 1-9],\n");
        prompt.append("  \"coherenceCohesion\": [score 1-9],\n");
        prompt.append("  \"lexicalResource\": [score 1-9],\n");
        prompt.append("  \"grammaticalRangeAccuracy\": [score 1-9],\n");
        prompt.append("  \"overallBand\": [score 1-9],\n");
        prompt.append("  \"examinerFeedback\": \"[detailed feedback]\",\n");
        prompt.append("  \"suggestions\": {\n");
        prompt.append("    \"taskResponse\": \"[specific suggestions]\",\n");
        prompt.append("    \"coherenceCohesion\": \"[specific suggestions]\",\n");
        prompt.append("    \"lexicalResource\": \"[specific suggestions]\",\n");
        prompt.append("    \"grammaticalRangeAccuracy\": \"[specific suggestions]\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private EvaluationResponse parseAiResponse(String aiResponse) {
        try {
            // In a real implementation, you would use proper JSON parsing
            // This is a simplified version
            Map<String, Object> responseMap = parseJsonResponse(aiResponse);

            return new EvaluationResponse(
                    Double.parseDouble(responseMap.get("taskResponse").toString()),
                    Double.parseDouble(responseMap.get("coherenceCohesion").toString()),
                    Double.parseDouble(responseMap.get("lexicalResource").toString()),
                    Double.parseDouble(responseMap.get("grammaticalRangeAccuracy").toString()),
                    Double.parseDouble(responseMap.get("overallBand").toString()),
                    responseMap.get("examinerFeedback").toString(),
                    (Map<String, String>) responseMap.get("suggestions")
            );
        } catch (Exception e) {
            return new EvaluationResponse(5.0, 5.0, 5.0, 5.0, 5.0,
                    "Could not evaluate properly. " + e.getMessage(),
                    Map.of(
                            "general", "Please check your essay format and try again."
                    )
            );
        }
    }

    private Map<String, Object> parseJsonResponse(String json) {
        // Simplified parsing - in real app use Jackson/Gson
        Map<String, Object> map = new HashMap<>();
        // Implement actual JSON parsing here
        return map;
    }

    private void trackEvaluationMetrics(EvaluationResponse response) {
        metrics.trackEvaluation(
                String.valueOf(response.overallBand()),
                "taskResponse", response.taskResponse()
        );
        metrics.trackEvaluation(
                String.valueOf(response.overallBand()),
                "coherenceCohesion", response.coherenceCohesion()
        );
        // Track other criteria similarly
    }
}