package org.nc.springaitest.services;

import com.opencsv.CSVReader;
import org.nc.springaitest.model.EssayDocument;
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

    private static final int BATCH_SIZE = 2;
    private static final int DELAY_MS = 3000;  // 2 second delay between batches

    @Autowired
    private File vectorStoreFile;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EssayPreprocessor preprocessor;

    public void loadCsvEssays() throws Exception {
        List<Document> documents = new ArrayList<>();
        System.out.println("🔍 Starting CSV loading process...");

        if (!csvFile.exists()) {
            throw new IllegalStateException("CSV file not found at: " + csvFile.getURI());
        }

        try (CSVReader reader = new CSVReader(new InputStreamReader(csvFile.getInputStream()))) {
            String[] header = reader.readNext();
            System.out.println("📋 CSV Header: " + Arrays.toString(header));

            int lineNumber = 1; // Track line numbers for error reporting
            int processedCount = 0;
            int skippedCount = 0;

            String[] line;
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                try {
                    // Validate line structure
                    if (line.length < 9) {
                        System.out.printf("🚫 Line %d: Skipped - Only %d columns found%n", lineNumber, line.length);
                        skippedCount++;
                        continue;
                    }

                    // Clean and validate task type
                    String taskType = line[0].trim();
                    if (!taskType.equalsIgnoreCase("2")) {
                        System.out.printf("🚫 Line %d: Skipped - Invalid task type: %s%n", lineNumber, taskType);
                        skippedCount++;
                        continue;
                    }

                    // Validate essential fields
                    String question = line[1].trim();
                    String rawEssay = line[2].trim();
                    String overallScore = line[8].trim();

                    if (question.isEmpty() || rawEssay.isEmpty() || overallScore.isEmpty()) {
                        System.out.printf("🚫 Line %d: Skipped - Missing required fields%n", lineNumber);
                        skippedCount++;
                        continue;
                    }

                    // Process content
                    String cleanEssay = preprocessor.cleanEssay(rawEssay);
                    String topic = preprocessor.extractMainTopic(question);
                    int wordCount = preprocessor.countWords(cleanEssay);

                    // Build document content
                    String content = buildDocumentContent(
                            question,
                            cleanEssay,
                            line[3],  // Examiner comment
                            line[4],    // TR score
                            line[5],    // CC score
                            line[6],    // LR score
                            line[7],    // GRA score
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
                    System.err.printf("❌ Error processing line %d: %s%n", lineNumber, e.getMessage());
                    System.err.println("Problematic line: " + Arrays.toString(line));
                    skippedCount++;
                }
            }

            // Final validation
            System.out.printf("\n📊 Processing Summary:%n");
            System.out.printf(" - Total lines processed: %d%n", lineNumber - 1);
            System.out.printf(" - Successfully processed: %d%n", processedCount);
            System.out.printf(" - Skipped lines: %d%n", skippedCount);

            if (documents.isEmpty()) {
                throw new IllegalStateException("No valid documents processed. Possible reasons:\n" +
                        "1. CSV format mismatch\n" +
                        "2. All entries filtered out\n" +
                        "3. File contains only header row");
            }


            int total = documents.size();
            for (int i = 0; i < total; i += BATCH_SIZE) {

                System.out.printf("Batch number: %d%n", (i / BATCH_SIZE) + 1);



                int end = Math.min(i + BATCH_SIZE, total);
                List<Document> batch = documents.subList(i, end);

                vectorStore.accept(batch);
                saveVectorStore();

                System.out.printf("Processed %d/%d documents (%.1f%%)%n",
                        end, total, (end * 100.0) / total);

                Thread.sleep(DELAY_MS);
            }

        } catch (Exception e) {
            System.err.println("💥 Critical error during CSV processing: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String buildDocumentContent(
            String question, String essay, String examinerComment,
            String trScore, String ccScore, String lrScore,
            String graScore, String overallScore) {

        return String.format(
                "IELTS Writing Task 2 Essay (Band %s)%n%nQuestion:%n%s%n%nEssay:%n%s%n%n" +
                        "Examiner Comments:%n%s%n%nScores:%n- Task Response: %s%n" +
                        "- Coherence & Cohesion: %s%n- Lexical Resource: %s%n" +
                        "- Grammatical Range & Accuracy: %s%n- Overall: %s",
                overallScore, question, essay, examinerComment,
                trScore, ccScore, lrScore, graScore, overallScore
        );
    }

    private void saveVectorStore() {
        if (!(vectorStore instanceof SimpleVectorStore)) {
            throw new IllegalStateException("Vector store is not SimpleVectorStore instance");
        }

        try {
            ((SimpleVectorStore) vectorStore).save(vectorStoreFile);
            System.out.println("💾 Saved vector store to: " + vectorStoreFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error saving vector store: " + e.getMessage());
            throw new RuntimeException("Vector store persistence failed", e);
        }
    }

}