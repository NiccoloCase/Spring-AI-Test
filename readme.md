# LangChain vs Spring AI: Dual RAG Systems for IELTS Essay Evaluation

This repository contains a comparative experiment implementing two Retrieval-Augmented Generation (RAG) systems for automated IELTS essay evaluation, one using **LangChain (Python)** and another using **Spring AI (Java)**. The project includes a frontend built with **React** and **Tailwind CSS**, and serves as a hands-on exploration of LLM integration workflows in both ecosystems.

---

## Tech Stack

### Backend Python

- **LangChain**
- FAISS
- OpenAI API
- FastAPI

### Backend Java

- **Spring AI**
- SimpleVectorStore
- OpenAI API

### Frontend

- React
- Tailwind

## Dataset

Dataset sourced from Kaggle:

- [IELTS Writing Scored Essays Dataset on Kaggle](https://www.kaggle.com/datasets/mazlumi/ielts-writing-scored-essays-dataset)
- 1200 IELTS essays from Task 1 and Task 2 annotated with band scores and examiner feedback
- Used for similarity-based retrieval
- A preprocessing was carried out to filter only Task 1, extract the main topic of each essay, and improve the overall text formatting.

---

## Features

- Dual RAG implementations (LangChain and Spring AI)
- IELTS dataset retrieval and context enrichment
- Essay evaluation via GPT-4
- React-based interface for input and display
- REST API integration

---

## Comparison Summary

| Feature              | LangChain (Python)       | Spring AI (Java)            |
| -------------------- | ------------------------ | --------------------------- |
| Setup Ease           | ✅ High                  | ⚠️ Moderate                 |
| Modularity           | ✅ High                  | ✅ High                     |
| Backend Integration  | ⚠️ Low                   | ✅ High                     |
| Community Support    | ✅ Active & Growing      | ⚠️ Emerging                 |
| Agent Support        | ✅ Native                | ❌ Not yet supported        |
| Deployment Readiness | ⚠️ Manual APIs (FastAPI) | ✅ Spring Boot/Cloud-native |

---

## Running the Project

### 1. Clone the Repo

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
cd YOUR_REPO_NAME
```

### 2. Backend (LangChain)

```bash
cd backend/python
pip install -r requirements.txt
python3 main.py
```

### 3. Backend (Spring AI)

```bash
cd backend/java
./mvnw spring-boot:run
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## ⚠️ Improvements Needed

This project is intended solely as a development testbed for experimenting with the development lifecycle with LangChain and Spring AI in the context of Retrieval-Augmented Generation (RAG). It is not production-ready. Several improvements are necessary to move towards production-quality product, including:

1. **Prompt Engineering:**

   - Reduce the number of in-context examples (currently five) to lower API usage costs.
   - Standardize the format of examples to align with the desired JSON output structure. This is currently complicated by the dataset’s lack of per-category evaluation scores.

2. **Hyperparameter Tuning:** Optimize the temperature setting to balance between creativity and consistency in output. Different temperature values can be used for evaluation (which should be deterministic), while human-like feedback can be used for more subjective assessment.

3. **Structured Output:** Utilize the structured output features of both Spring AI and LangChain to ensure that model responses strictly conform to the predefined JSON schema.

4. **Deployment Considerations:** Replace the in-memory vector store used in prototypes with a production-ready, deployed vector database to enable scalable retrieval-augmented generation.

## Report

You can read the report [here](./report.pdf).
