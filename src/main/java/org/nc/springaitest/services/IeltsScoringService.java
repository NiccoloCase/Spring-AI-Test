package org.nc.springaitest.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nc.springaitest.dto.EssayRequest;
import org.nc.springaitest.dto.EvaluationResponse;
import org.nc.springaitest.model.EssayDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

        System.out.println("Found similar essays: " + similarEssays.size());


        // Build prompt
        String prompt = buildScoringPrompt(request, cleanedEssay, similarEssays);

        System.out.println("Prompt for AI: " + prompt);

        // Get AI evaluation
        String aiResponse = chatModel.call(prompt);

        System.out.println("AI response: " + aiResponse);

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
                if (doc != null) {
                    prompt.append(" Example ---\n").append(doc.getText()).append("\n\n");
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



    /**
     * Parses the AI response JSON and returns an EvaluationResponse.
     *
     * @param aiResponse the raw response from the AI model
     * @return an EvaluationResponse with parsed values, or a default error response if parsing fails
     */
    public EvaluationResponse parseAiResponse(String aiResponse) {
        try {
            // Remove any extraneous text before the first '{'
            String json = aiResponse.trim();
            int index = json.indexOf("{");
            if (index > 0) {
                json = json.substring(index);
            }

            Map<String, Object> responseMap = parseJsonResponse(json);

            // Debug print
            System.out.println("Parsed response map: " + responseMap);

            double taskResponse = getDoubleFromMap(responseMap, "taskResponse", 5.0);
            double coherenceCohesion = getDoubleFromMap(responseMap, "coherenceCohesion", 5.0);
            double lexicalResource = getDoubleFromMap(responseMap, "lexicalResource", 5.0);
            double grammaticalRangeAccuracy = getDoubleFromMap(responseMap, "grammaticalRangeAccuracy", 5.0);
            double overallBand = getDoubleFromMap(responseMap, "overallBand", 5.0);

            // Get examiner feedback or a default string.
            String examinerFeedback = responseMap.get("examinerFeedback") != null
                    ? responseMap.get("examinerFeedback").toString()
                    : "No feedback provided";

            // Cast the suggestions object to Map<String, String>
            Map<String, String> suggestions = castSuggestions(responseMap.get("suggestions"));

            return new EvaluationResponse(
                    taskResponse,
                    coherenceCohesion,
                    lexicalResource,
                    grammaticalRangeAccuracy,
                    overallBand,
                    examinerFeedback,
                    suggestions
            );
        } catch (Exception e) {
            return new EvaluationResponse(
                    5.0, 5.0, 5.0, 5.0, 5.0,
                    "Could not evaluate properly. " + e.getMessage(),
                    Map.of("general", "Please check your essay format and try again.")
            );
        }
    }

    /**
     * Uses Jackson's ObjectMapper to convert the given JSON string into a Map.
     *
     * @param json the JSON string to parse
     * @return a map containing the parsed key/value pairs
     * @throws IOException if parsing fails
     */
    private Map<String, Object> parseJsonResponse(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Helper to extract a double value from the given map.
     *
     * @param map the data map
     * @param key the key to look for
     * @param defaultValue the value to return if the key is missing or malformatted
     * @return the parsed double or defaultValue
     */
    private double getDoubleFromMap(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        } else if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Safely casts the suggestions object into a Map<String, String>.
     *
     * @param suggestionsRaw the raw object for suggestions
     * @return a map with suggestions; if casting fails, a default map is returned.
     */
    private Map<String, String> castSuggestions(Object suggestionsRaw) {
        Map<String, String> result = new HashMap<>();
        if (suggestionsRaw instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
        if (result.isEmpty()) {
            result.put("general", "Please check your essay format and try again.");
        }
        return result;
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

    }
}