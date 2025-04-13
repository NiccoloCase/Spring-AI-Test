import csv
import os
import time
from pathlib import Path
from typing import Any
from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings

class CsvIeltsTask2Loader:
    def __init__(self, csv_path: str, vector_store_path: str, preprocessor: Any):
        self.csv_path = csv_path
        self.vector_store_path = Path(vector_store_path)
        self.preprocessor = preprocessor
        self.batch_size = 100
        self.delay_seconds = 5
        self.embeddings = OpenAIEmbeddings()

    def load_csv_essays(self):
        '''Loads essays from a CSV file, processes them, and stores them in a vector store.
        Raises:
            FileNotFoundError: If the CSV file does not exist.
            ValueError: If no valid documents are found in the CSV.
        '''

        # Check if the CSV file exists
        if not os.path.exists(self.csv_path):
            raise FileNotFoundError(f"CSV file not found at: {self.csv_path}")
        
        # Check if the vector store is already loaded
        if self._is_vector_store_loaded():
            print("Vector store already loaded, skipping CSV processing.")
            return

        documents = []

        with open(self.csv_path, newline='', encoding='utf-8') as csvfile:
            reader = csv.reader(csvfile)
            header = next(reader)
            print(f"CSV Header: {header}")

            processed_count = 0
            skipped_count = 0

            for line_num, row in enumerate(reader, 2):
                try:
                    if len(row) < 9:
                        print(f"Line {line_num}: Skipped - Only {len(row)} columns found")
                        skipped_count += 1
                        continue

                    # Check if the task type is 2 (filter the dataset)
                    if row[0].strip().lower() != "2":
                        skipped_count += 1
                        continue

                    question = row[1].strip()
                    raw_essay = row[2].strip()
                    overall_score = row[8].strip()

                    if not all([question, raw_essay, overall_score]):
                        print(f"Line {line_num}: Skipped - Missing required fields")
                        skipped_count += 1
                        continue

                    clean_essay = self.preprocessor.clean_essay(raw_essay)
                    topic = self.preprocessor.extract_main_topic(question)
                    word_count = self.preprocessor.count_words(clean_essay)

                    content = self._build_document_content(
                        question, clean_essay, row[3], overall_score
                    )

                    metadata = {
                        "type": "task2_essay",
                        "band": overall_score,
                        "question": question,
                        "topic": topic,
                        "word_count": word_count,
                        "source_line": line_num
                    }

                    documents.append(Document(page_content=content, metadata=metadata))
                    processed_count += 1

                except Exception as e:
                    print(f"Error processing line {line_num}: {str(e)}")
                    skipped_count += 1

        print(f"Processing Summary:\n - Processed: {processed_count}\n - Skipped: {skipped_count}")

        if not documents:
            raise ValueError("No valid documents found in CSV")

        # Init FAISS
        vector_store = FAISS.from_documents(documents[:self.batch_size], self.embeddings)
        remaining = documents[self.batch_size:]

        # Process in batches
        for i in range(0, len(remaining), self.batch_size):
            batch = remaining[i:i+self.batch_size]
            vector_store.add_documents(batch)
            vector_store.save_local(self.vector_store_path)

            print(f"Processed {i + self.batch_size + len(batch)}/{len(documents)} documents")

            time.sleep(self.delay_seconds)


        # Save the vector store
        vector_store.save_local(self.vector_store_path)



    def _is_vector_store_loaded(self) -> bool:
        '''Checks if the vector store is already loaded by looking for the index file.
        Returns:
            bool: True if the vector store is loaded, False otherwise.
        '''
        return (self.vector_store_path / "index.faiss").exists()
    


    def _build_document_content(self, question: str, essay: str, examiner_comment: str, score: str) -> str:
        '''Builds the content for the document to be stored in the vector store.
        Args:
            question (str): The essay question.
            essay (str): The essay text.
            examiner_comment (str): Comments from the examiner.
            score (str): The band score.
        Returns:
            str: The formatted content string.
        '''

        content = f"IELTS Writing Task 2 Essay (Band {score})\n\nQuestion:\n{question}\n\nEssay:\n{essay}\n\n"
        if examiner_comment.strip():
            content += f"Examiner Comments:\n{examiner_comment}\n\n"
        content += f"Scores:\n- Overall: {score}"
        return content