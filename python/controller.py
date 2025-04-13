from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from EssayPreprocessor import EssayPreprocessor
from IeltsScoringService import IeltsScoringService

app = FastAPI()


class EssayRequest(BaseModel):
    question: str = Field(..., min_length=10, max_length=2000)
    essay: str = Field(..., min_length=100, max_length=6000)
    task_type: str = "2"


preprocessor = EssayPreprocessor()
service = IeltsScoringService("data/vector_store", preprocessor)


@app.post("/ai/scoreEssay")
async def score_essay(request: EssayRequest):
    
    try:
        return service.score_essay(request.model_dump())
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))