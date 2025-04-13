package org.nc.IELTSChecker.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class VectorStoreConfig {
    @Value("${vector.store.path:data/vector_store.json}")

    private String vectorStorePath;

    @Bean
    public SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore =  SimpleVectorStore.builder(embeddingModel).build();
        File vectorStoreFile = getVectorStoreFile();

        try {
            if (vectorStoreFile.exists() && vectorStoreFile.length() > 0) {
                vectorStore.load(vectorStoreFile);
                System.out.println("Loaded existing vector store from: "
                        + vectorStoreFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Error loading vector store: " + e.getMessage());
        }

        return vectorStore;
    }

    @Bean
    public File vectorStoreFile() {
        return getVectorStoreFile();
    }

    private File getVectorStoreFile() {
        File file = Paths.get(vectorStorePath).toAbsolutePath().normalize().toFile();
        file.getParentFile().mkdirs();
        return file;
    }
}