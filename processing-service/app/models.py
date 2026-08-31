# pyrefly: ignore [missing-import]
from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional, Dict, Any
from datetime import datetime

class BaseCamelModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True, serialize_by_alias=True)

class EmailAttachment(BaseCamelModel):
    filename: str
    content_type: Optional[str] = Field(default=None, alias="contentType")
    attachment_id: Optional[str] = Field(default=None, alias="attachmentId")
    byte_size: Optional[int] = Field(default=None, alias="byteSize")

class EmailPayload(BaseCamelModel):
    message_id: str = Field(..., alias="messageId")
    source_email: Optional[str] = Field(default=None, alias="sourceEmail")
    thread_id: Optional[str] = Field(default=None, alias="threadId")
    subject: Optional[str] = Field(default="", alias="subject")
    sender: Optional[str] = Field(default="", alias="sender")
    recipients: Optional[str] = Field(default="", alias="recipients")
    plain_text_body: Optional[str] = Field(default="", alias="plainTextBody")
    html_body: Optional[str] = Field(default=None, alias="htmlBody")
    snippet: Optional[str] = Field(default=None, alias="snippet")
    received_at: Optional[str] = Field(default=None, alias="receivedAt")
    attachments: Optional[List[EmailAttachment]] = Field(default_factory=list)

class EligibilityModel(BaseCamelModel):
    minimum_cgpa: Optional[float] = Field(default=None, alias="minimumCgpa")
    maximum_cgpa: Optional[float] = Field(default=None, alias="maximumCgpa")
    allowed_branches: Optional[List[str]] = Field(default=None, alias="allowedBranches")
    allowed_specializations: Optional[List[str]] = Field(default=None, alias="allowedSpecializations")
    allowed_batches: Optional[List[str]] = Field(default=None, alias="allowedBatches")
    allowed_degrees: Optional[List[str]] = Field(default=None, alias="allowedDegrees")
    gender_restriction: Optional[str] = Field(default=None, alias="genderRestriction")
    no_standing_arrears: Optional[bool] = Field(default=None, alias="noStandingArrears")
    other_conditions: Optional[List[str]] = Field(default=None, alias="otherConditions")

class RoleModel(BaseCamelModel):
    title: str = Field(..., alias="title")
    description: Optional[str] = Field(default=None, alias="description")
    role_order: int = Field(default=1, alias="roleOrder")
    eligibility: Optional[EligibilityModel] = Field(default=None, alias="eligibility")
    evidence: Optional[Dict[str, str]] = Field(default=None, alias="evidence")

class ImportantDateModel(BaseCamelModel):
    description: str
    date: Optional[str] = None
    raw_text: Optional[str] = Field(default=None, alias="rawText")

class ClassificationResult(BaseCamelModel):
    is_placement: bool = Field(..., alias="isPlacement")
    confidence: float = Field(default=1.0, alias="confidence")
    evidence: List[str] = Field(default_factory=list, alias="evidence")

class ExtractionResult(BaseCamelModel):
    message_id: str = Field(..., alias="messageId")
    source_email: Optional[str] = Field(default=None, alias="sourceEmail")
    classification: ClassificationResult
    company_name: Optional[str] = Field(default=None, alias="companyName")
    drive_title: Optional[str] = Field(default=None, alias="driveTitle")
    description: Optional[str] = Field(default=None, alias="description")
    application_deadline: Optional[str] = Field(default=None, alias="applicationDeadline")
    important_dates: Optional[List[ImportantDateModel]] = Field(default_factory=list, alias="importantDates")
    common_eligibility: Optional[EligibilityModel] = Field(default=None, alias="commonEligibility")
    roles: Optional[List[RoleModel]] = Field(default_factory=list, alias="roles")
    field_evidence: Optional[Dict[str, str]] = Field(default_factory=dict, alias="fieldEvidence")
    error_message: Optional[str] = Field(default=None, alias="errorMessage")

class DocumentCandidate(BaseCamelModel):
    registration_number: Optional[str] = Field(default=None, alias="registrationNumber")
    neopat_id: Optional[str] = Field(default=None, alias="neopatId")
    name: Optional[str] = Field(default=None, alias="name")
    role: Optional[str] = Field(default=None, alias="role")
    evidence: Optional[str] = Field(default=None, alias="evidence")

class DocumentProcessingResult(BaseCamelModel):
    document_id: Optional[str] = Field(default=None, alias="documentId")
    attachment_id: Optional[int] = Field(default=None, alias="attachmentId")
    filename: str = Field(..., alias="filename")
    document_type: str = Field(..., alias="documentType")
    classification: str = Field(..., alias="classification")
    confidence: float = Field(default=1.0, alias="confidence")
    ocr_required: bool = Field(default=False, alias="ocrRequired")
    candidates: List[DocumentCandidate] = Field(default_factory=list, alias="candidates")
    error_message: Optional[str] = Field(default=None, alias="errorMessage")

