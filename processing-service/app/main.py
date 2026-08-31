# pyrefly: ignore [missing-import]
from fastapi import FastAPI, HTTPException
from app.models import EmailPayload, ClassificationResult, ExtractionResult
from app.classifier import EmailClassifier
from app.extractor import PlacementExtractor
from app.llm_extractor import LLMExtractor

app = FastAPI(title="PlacementOS Processing Service")

classifier = EmailClassifier()
extractor = PlacementExtractor()
llm_extractor = LLMExtractor()

@app.get("/health")
def health_check():
    return {"status": "UP"}

@app.post("/api/v1/classify", response_model=ClassificationResult)
def classify_email(payload: EmailPayload):
    return classifier.classify(payload)

@app.post("/api/v1/extract", response_model=ExtractionResult)
def extract_placement_info(payload: EmailPayload):
    classification = classifier.classify(payload)
    return extractor.extract(payload, classification)

@app.post("/api/v1/process", response_model=ExtractionResult)
def process_email(payload: EmailPayload):
    """
    Main processing endpoint: classifies email and extracts structured placement information
    using deterministic parsing with LLM fallback where appropriate.
    """
    return llm_extractor.extract_with_fallback(payload)
