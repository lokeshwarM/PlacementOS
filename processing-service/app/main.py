# pyrefly: ignore [missing-import]
from fastapi import FastAPI

app = FastAPI(title="PlacementOS Processing Service")

@app.get("/health")
def health_check():
    return {"status": "UP"}
