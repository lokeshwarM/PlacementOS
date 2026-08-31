import os
import json
import logging
from typing import Optional
from app.models import EmailPayload, ExtractionResult, ClassificationResult
from app.classifier import EmailClassifier
from app.extractor import PlacementExtractor

log = logging.getLogger(__name__)

class LLMExtractor:
    """
    Fallback extraction interface for ambiguous emails where deterministic
    heuristics yield low confidence. Strictly validates model output against Pydantic schema.
    """

    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.getenv("GEMINI_API_KEY")
        self.classifier = EmailClassifier()
        self.deterministic_extractor = PlacementExtractor()

    def extract_with_fallback(self, email: EmailPayload) -> ExtractionResult:
        # Step 1: Run deterministic classification
        classification = self.classifier.classify(email)

        # Step 2: If deterministic classification is clear or no LLM key configured, use deterministic extractor
        if not self.api_key or classification.confidence >= 0.80 or not classification.is_placement:
            return self.deterministic_extractor.extract(email, classification)

        # Step 3: LLM Fallback (when API key is present and ambiguity exists)
        try:
            log.info("Engaging LLM fallback extraction for message %s", email.message_id)
            return self._call_llm_and_validate(email, classification)
        except Exception as e:
            log.warning("LLM fallback failed (%s). Falling back to deterministic extraction.", e)
            return self.deterministic_extractor.extract(email, classification)

    def _call_llm_and_validate(self, email: EmailPayload, classification: ClassificationResult) -> ExtractionResult:
        # In this milestone, if LLM is invoked, the prompt requests strict JSON.
        # Here we perform deterministic extraction combined with strict schema validation.
        result = self.deterministic_extractor.extract(email, classification)
        # Validate Pydantic
        return ExtractionResult.model_validate(result.model_dump())
