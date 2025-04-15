# LangChain vs Spring AI: Dual RAG Systems for IELTS Essay Evaluation

This repository contains a comparative experiment implementing two Retrieval-Augmented Generation (RAG) systems for automated IELTS essay evaluation, one using **LangChain (Python)** and another using **Spring AI (Java)**. The project includes a frontend built with **React** and **Tailwind CSS**, and serves as a hands-on exploration of LLM integration workflows in both ecosystems.

## Motivation

With the rise of large language models (LLMs), integrating them effectively into production systems is critical. This project investigates the developer experience of the two modern frameworks:

- **LangChain** (Python)
- **Spring AI** (Java)

The goal is to assess their utility in a practical NLP application: **IELTS Essay Scoring** using Retrieval-Augmented Generation.

---

## Tech Stack

### Backend Python

- **LangChain**
- FAISS
- OpenAI
- FastAPI

### Backend Java

- **Spring AI**
- SimpleVectorStore
- OpenAI API

### Frontend

- React
- Tailwind

## Dataset

Dataset sourced from Kaggle: ~1200 IELTS essays annotated with band scores and evaluation criteria.

- [IELTS Writing Scored Essays Dataset on Kaggle](https://www.kaggle.com/datasets/mazlumi/ielts-writing-scored-essays-dataset)
- Essays from Task 1 and Task 2

---

## Features

- Dual RAG implementations (LangChain and Spring AI)
- IELTS dataset retrieval and context enrichment
- Essay evaluation via GPT-4
- React-based interface for input and display
- REST API integration

---

## ⚖️ Comparison Summary

| Feature              | LangChain (Python)       | Spring AI (Java)            |
| -------------------- | ------------------------ | --------------------------- |
| Setup Ease           | ✅ High                  | ⚠️ Moderate                 |
| Modularity           | ✅ High                  | ✅ High                     |
| Backend Integration  | ⚠️ Low                   | ✅ High                     |
| Community Support    | ✅ Active & Growing      | ⚠️ Emerging                 |
| Agent Support        | ✅ Native                | ❌ Not yet supported        |
| Deployment Readiness | ⚠️ Manual APIs (FastAPI) | ✅ Spring Boot/Cloud-native |

---

## 🚀 Running the Project

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

## Improvements Needed

1. **Prompt Engineering**

   - Reduce in-context examples to cut API cost
   - Standardize example formatting

2. **Hyperparameter Tuning**

   - Optimize temperature and top_p for evaluation vs feedback modes

3. **Structured Output**

   - Use JSON schema enforcement features of LangChain/Spring AI

4. **Production Deployment**
   - Replace SimpleVectorStore with a persistent vector DB (e.g., Pinecone, Weaviate)

## ⚠️ Disclaimer

This is a proof-of-concept for development and evaluation only. Not intended for production use without further optimization.

## Report

You can read the report [here](./report.pdf).
