import os
from typing import Optional, List
from app.models import DocumentProcessingResult, DocumentCandidate
from app.doc_classifier import classify_document
from app.excel_parser import parse_excel_document
from app.pdf_parser import parse_pdf_document
from app.docx_parser import parse_docx_document

def process_document(attachment_id: Optional[int],
                     filename: str,
                     content_bytes: bytes) -> DocumentProcessingResult:
    """
    Main document processing orchestrator. Identifies file type, extracts candidate data,
    and returns a structured DocumentProcessingResult.
    """
    if not filename:
        filename = "document.bin"

    ext = os.path.splitext(filename)[1].lower()
    candidates: List[DocumentCandidate] = []
    ocr_required = False
    sample_text = ""
    doc_type = "OTHER"

    if ext in [".xlsx", ".xls"]:
        doc_type = "XLSX"
        candidates = parse_excel_document(content_bytes)
    elif ext == ".pdf":
        doc_type = "PDF"
        candidates, ocr_required, sample_text = parse_pdf_document(content_bytes)
    elif ext in [".docx", ".doc"]:
        doc_type = "DOCX"
        candidates = parse_docx_document(content_bytes)
    elif ext == ".csv":
        doc_type = "CSV"
        candidates = parse_excel_document(content_bytes)
    else:
        doc_type = "UNSUPPORTED"

    # Classify document
    classification, confidence = classify_document(filename, sample_text)

    # If candidates were successfully extracted with strong identifiers, confirm shortlist classification
    if candidates and classification not in ["JOB_DESCRIPTION", "RECRUITMENT_PROCESS"]:
        classification = "SHORTLIST"
        confidence = max(confidence, 0.95)

    return DocumentProcessingResult(
        document_id=f"doc-{attachment_id or 0}-{os.path.basename(filename)}",
        attachment_id=attachment_id,
        filename=filename,
        document_type=doc_type,
        classification=classification,
        confidence=confidence,
        ocr_required=ocr_required,
        candidates=candidates,
        error_message=None if doc_type != "UNSUPPORTED" else f"Unsupported document format: {ext}"
    )
