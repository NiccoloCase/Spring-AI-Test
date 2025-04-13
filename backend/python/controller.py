from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from fastapi.middleware.cors import CORSMiddleware
from pathlib import Path

from EssayPreprocessor import EssayPreprocessor
from IeltsScoringService import IeltsScoringService


app = FastAPI()

#  CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # Your React app's origin
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class EssayRequest(BaseModel):
    question: str = Field(..., min_length=10, max_length=2000)
    essay: str = Field(..., min_length=100, max_length=6000)
    task_type: str = "2"


preprocessor = EssayPreprocessor()
service = IeltsScoringService(
    str(Path(__file__).parent / "data/vector_store"),
    preprocessor
)


@app.post("/ai/scoreEssay")
async def score_essay(request: EssayRequest):
    
    try:
        return service.score_essay(request.model_dump())
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))