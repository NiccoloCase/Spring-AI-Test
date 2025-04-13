package org.nc.IELTSChecker.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nc.IELTSChecker.dto.EssayRequest;
import org.nc.IELTSChecker.dto.EvaluationResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
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
    private final OpenAiChatModel chatModel;

    @Autowired
    private EssayPreprocessor preprocessor;


    @Autowired
    public IeltsScoringService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Scores an essay based on the provided request.
     * @param request the EssayRequest containing the essay and question
     * @return EvaluationResponse containing the scores and feedback
     */
    public EvaluationResponse scoreEssay(EssayRequest request) {
        System.out.println("***************************************************************************************");
        System.out.println("***************************************************************************************");


        String cleanedEssay = preprocessor.cleanEssay(request.essay());
        String searchQuery = request.question() + "\n" + cleanedEssay;

        // RAG
        List<Document> similarEssays = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchQuery)
                        .topK(5)
                        .similarityThreshold(0.7)
                        .build()
        );



        System.out.println("Found similar essays: " + similarEssays.size());
        System.out.println("---------------------------------------------------------------------------------------");

        // Build prompt
        String prompt = buildScoringPrompt(request, cleanedEssay, similarEssays);

        System.out.println("Prompt for AI: " + prompt);
        System.out.println("---------------------------------------------------------------------------------------");
        // Get Model response
        String aiResponse = chatModel.call(prompt);

        System.out.println("AI response: " + aiResponse);

        // Parse response
        return parseAiResponse(aiResponse);
    }


    /**
     * Builds the scoring prompt for the AI model.
     * @param request the EssayRequest containing the essay and question
     * @param essay the cleaned essay text
     * @param examples the list of similar essays for reference (RAG)
     * @return the constructed prompt string
     */
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
                    1.0, 1.0, 1.0, 1.0, 1.0,
                    "Could not evaluate properly. " + e.getMessage(),
                    Map.of("general", "Please check your essay format and try again.")
            );
        }
    }

    /**
     * Parses the JSON response string into a Map<String, Object>.
     * @param json the JSON string to parse
     * @return a map with parsed values
     * @throws IOException
     */
    private Map<String, Object> parseJsonResponse(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Safely retrieves a double value from the map, with a default value if the key is not found or if parsing fails.
     * @param map
     * @param key
     * @param defaultValue
     * @return
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
     * Safely casts the examiner's suggestions object into a Map<String, String>.
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
}