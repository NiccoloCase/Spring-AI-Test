package org.nc.IELTSChecker.services;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EssayPreprocessor {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\s([.,;:!?])");
    private static final List<String> QUESTION_WORDS = Arrays.asList(
            "discuss", "to what extent", "advantages", "disadvantages",
            "opinion", "view", "agree", "disagree"
    );

    /**
     * Cleans the essay by removing extra whitespace and punctuation.
     * @param essay
     * @return
     */
    public String cleanEssay(String essay) {
        if (essay == null) return "";

        String cleaned = WHITESPACE_PATTERN.matcher(essay).replaceAll(" ");
        cleaned = PUNCTUATION_PATTERN.matcher(cleaned).replaceAll("$1");
        return cleaned.trim();
    }

    /**
     * Counts the number of words in the essay.
     * @param text
     * @return
     */
    public int countWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.split("\\s+").length;
    }

    /**
     * Extracts the main topic from the question.
     * @param question
     * @return
     */
    public String extractMainTopic(String question) {
        if (question == null) return "general";

        String lowercase = question.toLowerCase();
        for (String word : QUESTION_WORDS) {
            lowercase = lowercase.replace(word, "");
        }

        String[] sentences = lowercase.split("[.,]");
        String firstSentence = sentences.length > 0 ? sentences[0] : "";
        return firstSentence.trim().isEmpty() ? "general" : firstSentence.trim();
    }
}