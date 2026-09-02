from typing import Optional
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

# pyrefly: ignore [missing-import]
from fastapi import UploadFile, File, Form
from app.models import DocumentProcessingResult
from app.document_processor import process_document
from app.doc_classifier import classify_document

@app.post("/api/v1/documents/process", response_model=DocumentProcessingResult)
async def process_document_endpoint(
    file: UploadFile = File(...),
    attachment_id: Optional[int] = Form(None)
):
    """
    Processes an uploaded document (Excel, PDF, DOCX), classifies intent,
    and extracts structured candidate shortlist records.
    """
    content = await file.read()
    return process_document(attachment_id, file.filename, content)

@app.post("/api/v1/documents/classify")
def classify_document_endpoint(filename: str, sample_text: Optional[str] = ""):
    classification, confidence = classify_document(filename, sample_text or "")
    return {"classification": classification, "confidence": confidence}

