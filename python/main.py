from dotenv import load_dotenv

# Load environment variables from .env file
# The .env filed should have the OPENAI_API_KEY variable
load_dotenv()

from controller import app


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3002)