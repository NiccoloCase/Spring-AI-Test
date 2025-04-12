package org.nc.IELTSChecker.model;

import org.springframework.ai.document.Document;
import java.util.Map;

public class EssayDocument extends Document {
    private final String bandScore;
    private final String question;
    private final String topic;

    public EssayDocument(String content, Map<String, Object> metadata,
                         String bandScore, String question, String topic) {
        super(content, metadata);
        this.bandScore = bandScore;
        this.question = question;
        this.topic = topic;
    }

    public String getBandScore() { return bandScore; }
    public String getQuestion() { return question; }
    public String getTopic() { return topic; }

    public String getContent() {
        return super.getText();
    }
}