from langchain_core.output_parsers import JsonOutputParser
from langchain_openai import ChatOpenAI
from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings
from pathlib import Path
from CsvIeltsTask2Loader import CsvIeltsTask2Loader


class IeltsScoringService:
    def __init__(self, vector_store_path: str, preprocessor: any):

        # Generate the vector store here if it doesn't exist
        if not Path(vector_store_path, "index.faiss").exists():
            print("No FAISS index found. Generating vector store...")
            try:
                loader = CsvIeltsTask2Loader(
                    csv_path= str(Path(__file__).parent / "data/ielts_writing_dataset.csv"),   
                    vector_store_path=vector_store_path,
                    preprocessor=preprocessor
                )
                loader.load_csv_essays()
            except Exception as e:
                print(f"Error while generating vector store: {e}")
                raise e
            
        # Load the vector store
        try:
            self.vector_store = FAISS.load_local(
                vector_store_path, 
                OpenAIEmbeddings(), 
                allow_dangerous_deserialization=True
            )
            print(f"Vector store loaded successfully from '{vector_store_path}'")
        except Exception as e:
            print(f"Failed to load vector store: {e}")
            raise e  

        self.preprocessor = preprocessor
        self.llm = ChatOpenAI(temperature=0.2, model="gpt-4")
        self.parser = JsonOutputParser()


    def score_essay(self, essay_request: dict) -> dict:
        """
        Scores an essay based on the provided essay request.

        Args:
            essay_request (dict): A dictionary containing the essay details. 
                Expected keys:
                    - 'essay' (str): The text of the essay to be scored.
                    - 'question' (str): The question or prompt associated with the essay.

        Returns:
            dict: A dictionary containing the scoring results, parsed from the 
            language model's response.
        """
        cleaned_essay = self.preprocessor.clean_essay(essay_request['essay'])
        search_query = f"{essay_request['question']}\n{cleaned_essay}"

        print("******************************************************************")
        print("******************************************************************")

        print(f"Essay to evaluate: {cleaned_essay}")
        print("---------------------------------------------------------------")


        similar_docs = self.vector_store.similarity_search(
            query=search_query,
            k=5,
            score_threshold=0.7
        )


        print(f"Similar documents found: {len(similar_docs)}")
        print("---------------------------------------------------------------")

        prompt = self._build_prompt(essay_request, cleaned_essay, similar_docs)


        print(f"Prompt to LLM: {prompt}")
        print("---------------------------------------------------------------")

        response = self.llm.invoke(prompt)

        print(f"LLM response: {response}")

        return self._parse_response(response.content)




   
    def _build_prompt(self, request: dict, essay: str, examples: list[Document]) -> str:
        """
        Constructs the prompt for the language model.
        Args:
            request (dict): The request dictionary containing the essay and question.
            essay (str): The essay text to be evaluated.
            examples (list[Document]): A list of example essays for reference.
        Returns:
            str: The constructed prompt string.
        """

        examples_text = "\n\n".join(
            f"<example>\n{doc.page_content.strip()}\n</example>" 
            for doc in examples
        )

        # Handle the reference examples section separately
        reference_section = ""
        if examples:
            reference_section = f"""
    <referenceExamples>
    {examples_text}
    </referenceExamples>
    """

        return f"""
    <system>
    You are an experienced IELTS examiner. Your job is to evaluate a student's essay following the IELTS Writing Task 2 scoring criteria.
    </system>

    <question>
    {request['question']}
    </question>

    <essay>
    {essay}
    </essay>

    <criteria>
    - Task Response (TR): Addresses all parts of the task, presents a clear position, supports ideas with relevant examples.
    - Coherence & Cohesion (CC): Sequences information logically, uses paragraphing and cohesive devices effectively.
    - Lexical Resource (LR): Uses a wide range of vocabulary appropriately, with accurate word choice and collocations.
    - Grammatical Range & Accuracy (GRA): Demonstrates a range of grammatical structures with a high level of accuracy.
    </criteria>

    {reference_section}

    <outputFormat>
    Return your evaluation strictly in the following JSON format:
    {{
    "taskResponse": 0-9,
    "coherenceCohesion": 0-9,
    "lexicalResource": 0-9,
    "grammaticalRangeAccuracy": 0-9,
    "overallBand": 0-9,
    "examinerFeedback": "string with detailed explanation (min 3 sentences)",
    "suggestions": {{
        "taskResponse": "brief improvement suggestion",
        "coherenceCohesion": "brief improvement suggestion",
        "lexicalResource": "brief improvement suggestion",
        "grammaticalRangeAccuracy": "brief improvement suggestion"
    }}
    }}
    </outputFormat>

    <instructions>
    Ensure the output is valid JSON. Do not include explanations outside the JSON. Be fair and objective in your assessment.
    </instructions>
    """



    def _parse_response(self, response: str) -> dict:
        """
        Parses the response from the language model.
        Args:
            response (str): The raw response from the language model.
        Returns:
            dict: The parsed response as a dictionary.
        """

        try:
            return self.parser.parse(response)
        except:
            return {
                "error": "Could not parse evaluation response",
                "raw_response": response
            }