package org.nc.IELTSChecker.services;

import com.opencsv.CSVReader;
import org.nc.IELTSChecker.model.EssayDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class CsvIeltsTask2Loader {

    @Value("classpath:/data/ielts_writing_dataset.csv")
    private Resource csvFile;

    private static final int BATCH_SIZE = 100;
    private static final int DELAY_MS = 5000;

    @Autowired
    private File vectorStoreFile;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EssayPreprocessor preprocessor;


    /**
     * Loads essays from a CSV file and processes them into a vector store.
     * @throws Exception If an error occurs during file reading or processing.
     */
    public void loadCsvEssays() throws Exception {
        List<Document> documents = new ArrayList<>();
        System.out.println("Starting IELTS Dataset loading process...");

        if (!csvFile.exists()) {
            throw new IllegalStateException("CSV file not found at: " + csvFile.getURI());
        }
        if(isVectorStoreAlreadyLoaded()) {
            System.out.println("Vector store already loaded, skipping CSV processing.");
            return;
        }

        try (CSVReader reader = new CSVReader(new InputStreamReader(csvFile.getInputStream()))) {
            String[] header = reader.readNext();
            System.out.println("CSV Header: " + Arrays.toString(header));

            int lineNumber = 1;
            int processedCount = 0;
            int skippedCount = 0;

            String[] line;
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                try {
                    if (line.length < 9) {
                        System.out.printf("Line %d: Skipped - Only %d columns found%n", lineNumber, line.length);
                        skippedCount++;
                        continue;
                    }

                    // Only process Task 2 essays
                    String taskType = line[0].trim();
                    if (!taskType.equalsIgnoreCase("2")) {
                        skippedCount++;
                        continue;
                    }

                    // Validate fields
                    String question = line[1].trim();
                    String rawEssay = line[2].trim();
                    String overallScore = line[8].trim();

                    if (question.isEmpty() || rawEssay.isEmpty() || overallScore.isEmpty()) {
                        System.out.printf("Line %d: Skipped - Missing required fields%n", lineNumber);
                        skippedCount++;
                        continue;
                    }

                    String cleanEssay = preprocessor.cleanEssay(rawEssay);
                    String topic = preprocessor.extractMainTopic(question);
                    int wordCount = preprocessor.countWords(cleanEssay);

                    String content = buildDocumentContent(
                            question,
                            cleanEssay,
                            line[3],    // Examiner comment
                            overallScore
                    );

                    // Create metadata
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("type", "task2_essay");
                    metadata.put("band", overallScore);
                    metadata.put("question", question);
                    metadata.put("topic", topic);
                    metadata.put("word_count", wordCount);
                    metadata.put("source_line", lineNumber);

                    // Create and store document
                    documents.add(new EssayDocument(content, metadata, overallScore, question, topic));
                    processedCount++;

                } catch (Exception e) {
                    System.err.printf("Error processing line %d: %s%n", lineNumber, e.getMessage());
                    System.err.println("Problematic line: " + Arrays.toString(line));
                    skippedCount++;
                }
            }

            System.out.printf("Processing Summary:%n");
            System.out.printf(" - Total lines processed: %d%n", lineNumber - 1);
            System.out.printf(" - Successfully processed: %d%n", processedCount);
            System.out.printf(" - Skipped lines: %d%n", skippedCount);


            if (documents.isEmpty()) throw new IllegalStateException("Error: No valid documents found in the CSV file.");

            // Process documents in batches
            System.out.println("Processing documents in batches...");
            int total = documents.size();
            for (int i = 0; i < total; i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, total);
                List<Document> batch = documents.subList(i, end);

                vectorStore.accept(batch);
                saveVectorStore();

                System.out.printf("Processed %d/%d documents (%.1f%%)%n",
                        end, total, (end * 100.0) / total);

                Thread.sleep(DELAY_MS);
            }
        } catch (Exception e) {
            System.err.println("Critical error during CSV processing: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Builds the content string for the essay document.
     * @param question
     * @param essay
     * @param examinerComment
     * @param overallScore
     * @return Returns the formatted content string.
     */
    private String buildDocumentContent(String question, String essay, String examinerComment, String overallScore) {

            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(String.format(
                    "IELTS Writing Task 2 Essay (Band %s)%n%nQuestion:%n%s%n%nEssay:%n%s%n%n",
                    overallScore, question, essay
            ));

            if (!examinerComment.isEmpty())
                contentBuilder.append(String.format("Examiner Comments:%n%s%n%n", examinerComment));

            contentBuilder.append(String.format("Scores:%n- Overall: %s", overallScore));

            return contentBuilder.toString();
        }


    /**
     * Saves the vector store to a file.
     */
    private void saveVectorStore() {
        if (!(vectorStore instanceof SimpleVectorStore))
            throw new IllegalStateException("Vector store is misconfigured. Expected SimpleVectorStore.");


        try {
            ((SimpleVectorStore) vectorStore).save(vectorStoreFile);
            System.out.println("Saved vector store to: " + vectorStoreFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error saving vector store: " + e.getMessage());
            throw new RuntimeException("Vector store persistence failed", e);
        }
    }

    /**
     * Checks if the vector store is already loaded.
     * @return Returns true if the vector store file exists and is not empty.
     */
    public boolean isVectorStoreAlreadyLoaded() {
        return vectorStoreFile.exists() && vectorStoreFile.length() > 0;
    }
}