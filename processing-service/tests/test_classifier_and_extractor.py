# pyrefly: ignore [missing-import]
import httpx
from app.models import EmailPayload, ClassificationResult, ExtractionResult
from app.classifier import EmailClassifier
from app.extractor import PlacementExtractor
from app.llm_extractor import LLMExtractor
from app.main import app
# pyrefly: ignore [missing-import]
import pytest

classifier = EmailClassifier()
extractor = PlacementExtractor()
llm_extractor = LLMExtractor()

# ==============================================================================
# Fixture 1: Non-placement club email
# ==============================================================================
def test_non_placement_club_email():
    payload = EmailPayload(
        message_id="msg-club-1",
        subject="Dance Club Auditions 2026 - Riviera",
        plain_text_body="Join the dance club! Auditions will be held on Friday at the auditorium. All students are welcome."
    )
    res = classifier.classify(payload)
    assert res.is_placement is False
    assert res.confidence >= 0.70

    ext = extractor.extract(payload, res)
    assert ext.company_name is None
    assert len(ext.roles) == 0

# ==============================================================================
# Fixture 2: Non-placement exam circular
# ==============================================================================
def test_non_placement_exam_circular():
    payload = EmailPayload(
        message_id="msg-exam-1",
        subject="Examination Timetable for Fall Semester 2026",
        plain_text_body="Dear students, please download your hall ticket from VTOP. The examination timetable is attached."
    )
    res = classifier.classify(payload)
    assert res.is_placement is False

# ==============================================================================
# Fixture 3: Simple single-role placement email
# ==============================================================================
def test_single_role_placement():
    payload = EmailPayload(
        message_id="msg-msft-1",
        subject="Placement Drive: Microsoft - Software Engineer (FTE 2027)",
        plain_text_body="""Microsoft is conducting a campus placement drive.
Role: Software Engineer
Minimum CGPA: 8.0
Eligible branches: CSE, IT
Apply before: September 5, 2026 at 6:00 PM
No standing arrears allowed."""
    )
    res = classifier.classify(payload)
    assert res.is_placement is True

    ext = extractor.extract(payload, res)
    assert ext.company_name == "Microsoft"
    assert len(ext.roles) == 1
    assert "Software Engineer" in ext.roles[0].title
    assert ext.common_eligibility is not None
    assert ext.common_eligibility.minimum_cgpa == 8.0
    assert "CSE" in ext.common_eligibility.allowed_branches
    assert "IT" in ext.common_eligibility.allowed_branches
    assert ext.common_eligibility.no_standing_arrears is True
    assert ext.application_deadline is not None
    assert "2026-09-05" in ext.application_deadline

# ==============================================================================
# Fixture 4: Multi-role placement email with common eligibility
# ==============================================================================
def test_multi_role_common_eligibility():
    payload = EmailPayload(
        message_id="msg-amzn-1",
        subject="Campus Recruitment Drive - Amazon",
        plain_text_body="""Amazon is visiting VIT for campus recruitment.
Roles offered:
1. Software Development Engineer
2. Quality Assurance Engineer
3. Data Analyst

Common eligibility:
Minimum CGPA: 7.5
Eligible branches: CSE, IT, ECE
2027 batch only.
Registration deadline: September 10, 2026 at 11:59 PM"""
    )
    res = classifier.classify(payload)
    assert res.is_placement is True

    ext = extractor.extract(payload, res)
    assert ext.company_name == "Amazon"
    assert len(ext.roles) == 3
    role_titles = [r.title for r in ext.roles]
    assert any("Software Development Engineer" in t for t in role_titles)
    assert any("Quality Assurance Engineer" in t for t in role_titles)
    assert any("Data Analyst" in t for t in role_titles)

    assert ext.common_eligibility.minimum_cgpa == 7.5
    assert "CSE" in ext.common_eligibility.allowed_branches
    assert "2027" in ext.common_eligibility.allowed_batches

# ==============================================================================
# Fixture 5: Role-specific eligibility
# ==============================================================================
def test_role_specific_eligibility():
    payload = EmailPayload(
        message_id="msg-goog-1",
        subject="Placement Drive: Google",
        plain_text_body="""Google campus recruitment drive.
Roles:
1. Software Engineer
2. Data Scientist

Common eligibility:
Minimum CGPA 7.0
2027 batch

For Data Scientist role:
Eligible branches: CSE
Specialization: Data Science and AI
Minimum CGPA 8.0"""
    )
    res = classifier.classify(payload)
    assert res.is_placement is True

    ext = extractor.extract(payload, res)
    assert len(ext.roles) >= 2
    # Find Data Scientist role
    ds_role = next((r for r in ext.roles if "data scientist" in r.title.lower()), None)
    assert ds_role is not None
    assert ds_role.eligibility is not None
    assert ds_role.eligibility.minimum_cgpa == 8.0
    assert "Data Science" in ds_role.eligibility.allowed_specializations

# ==============================================================================
# Fixture 6: Minimum CGPA only (no branches invented)
# ==============================================================================
def test_minimum_cgpa_only_no_branch_invented():
    payload = EmailPayload(
        message_id="msg-cgpa-1",
        subject="Placement Drive - Acme Corp",
        plain_text_body="Acme Corp hiring. Minimum CGPA: 7.0. Apply before September 1, 2026."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert ext.common_eligibility.minimum_cgpa == 7.0
    assert ext.common_eligibility.allowed_branches is None  # Must NOT infer branches

# ==============================================================================
# Fixture 7: Branch-specific only (no CGPA invented)
# ==============================================================================
def test_branch_only_no_cgpa_invented():
    payload = EmailPayload(
        message_id="msg-branch-1",
        subject="Placement Drive - Widget Inc",
        plain_text_body="Widget Inc hiring. Eligible branches: CSE and ECE. Apply soon."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert "CSE" in ext.common_eligibility.allowed_branches
    assert "ECE" in ext.common_eligibility.allowed_branches
    assert ext.common_eligibility.minimum_cgpa is None  # Must NOT infer CGPA

# ==============================================================================
# Fixture 8: Multiple important dates
# ==============================================================================
def test_multiple_important_dates():
    payload = EmailPayload(
        message_id="msg-dates-1",
        subject="Campus Drive: Oracle",
        plain_text_body="""Oracle recruitment drive.
Apply before: September 3, 2026 at 6:00 PM
Online test: September 5, 2026
Interview date: September 8, 2026"""
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert ext.application_deadline is not None
    assert len(ext.important_dates) >= 2
    descriptions = [d.description for d in ext.important_dates]
    assert "Online Assessment" in descriptions
    assert "Interview Schedule" in descriptions

# ==============================================================================
# Fixture 9: Missing role falls back cleanly to title
# ==============================================================================
def test_missing_role_fallback():
    payload = EmailPayload(
        message_id="msg-norole-1",
        subject="Placement Drive: Uber",
        plain_text_body="Uber is conducting campus hiring. Minimum CGPA 8.0."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert len(ext.roles) == 1
    assert "Uber" in ext.roles[0].title or "Placement" in ext.roles[0].title

# ==============================================================================
# Fixture 10: Missing eligibility has null common_eligibility
# ==============================================================================
def test_missing_eligibility():
    payload = EmailPayload(
        message_id="msg-noelig-1",
        subject="Placement Drive: Cisco Information Session",
        plain_text_body="Cisco will conduct an information session. Roles: Software Engineer."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert ext.common_eligibility is None

# ==============================================================================
# Fixture 11: Bounded evidence length
# ==============================================================================
def test_bounded_evidence():
    long_text = "Microsoft is conducting a campus placement drive. " + ("Extra details here. " * 50) + "Minimum CGPA: 7.5."
    payload = EmailPayload(
        message_id="msg-long-1",
        subject="Placement Drive: Microsoft",
        plain_text_body=long_text
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    for k, v in ext.field_evidence.items():
        assert len(v) <= 150

# ==============================================================================
# Fixture 12: Pydantic camelCase serialization for Spring Boot
# ==============================================================================
def test_pydantic_camel_case_serialization():
    payload = EmailPayload(
        message_id="msg-serial-1",
        subject="Placement Drive: Intel",
        plain_text_body="Intel campus drive. Minimum CGPA 7.0."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    dumped = ext.model_dump(by_alias=True)
    assert "messageId" in dumped
    assert "companyName" in dumped
    assert "commonEligibility" in dumped
    assert "minimumCgpa" in dumped["commonEligibility"]

# ==============================================================================
# Fixture 13: FastAPI endpoints testing
# ==============================================================================
@pytest.mark.asyncio
async def test_fastapi_endpoints():
    payload_dict = {
        "messageId": "msg-api-1",
        "subject": "Placement Drive: Netflix",
        "plainTextBody": "Netflix hiring SDE. Minimum CGPA 8.5. Eligible branches: CSE, IT."
    }

    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as ac:
        # Test /health
        r = await ac.get("/health")
        assert r.status_code == 200
        assert r.json() == {"status": "UP"}

        # Test /api/v1/classify
        r = await ac.post("/api/v1/classify", json=payload_dict)
        assert r.status_code == 200
        assert r.json()["isPlacement"] is True

        # Test /api/v1/process
        r = await ac.post("/api/v1/process", json=payload_dict)
        assert r.status_code == 200
        data = r.json()
        assert data["companyName"] == "Netflix"
        assert data["commonEligibility"]["minimumCgpa"] == 8.5

# ==============================================================================
# Fixture 14: Batch extraction
# ==============================================================================
def test_batch_extraction():
    payload = EmailPayload(
        message_id="msg-batch-1",
        subject="Placement Drive: IBM",
        plain_text_body="IBM hiring 2027 batch. Minimum CGPA: 7.0."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert ext.common_eligibility.allowed_batches == ["2027"]

# ==============================================================================
# Fixture 15: No standing arrears
# ==============================================================================
def test_arrears_extraction():
    payload = EmailPayload(
        message_id="msg-arrears-1",
        subject="Campus Recruitment - Qualcomm",
        plain_text_body="Qualcomm hiring. Minimum CGPA: 8.0. No standing arrears allowed."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert ext.common_eligibility.no_standing_arrears is True

# ==============================================================================
# Fixture 16: Specialization without branch
# ==============================================================================
def test_specialization_without_branch():
    payload = EmailPayload(
        message_id="msg-spec-1",
        subject="Placement Drive: Nvidia",
        plain_text_body="Nvidia hiring. Artificial Intelligence and Cloud Computing specialization students are eligible."
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert "Artificial Intelligence" in ext.common_eligibility.allowed_specializations
    assert "Cloud Computing" in ext.common_eligibility.allowed_specializations
    assert ext.common_eligibility.allowed_branches is None

# ==============================================================================
# Fixture 17: Malformed payload rejection
# ==============================================================================
def test_malformed_payload_rejection():
    with pytest.raises(Exception):
        # message_id is required
        EmailPayload(subject="Placement Drive")

# ==============================================================================
# Fixture 18: Mixed common + role-specific eligibility
# ==============================================================================
def test_mixed_common_and_role_specific():
    payload = EmailPayload(
        message_id="msg-mix-1",
        subject="Placement Drive: Apple",
        plain_text_body="""Apple campus hiring.
Roles:
1. Hardware Engineer
2. Software Engineer

Common eligibility:
Minimum CGPA: 8.5
2027 batch

For Hardware Engineer role:
Eligible branches: ECE, EEE

For Software Engineer role:
Eligible branches: CSE, IT"""
    )
    res = classifier.classify(payload)
    ext = extractor.extract(payload, res)
    assert ext.common_eligibility.minimum_cgpa == 8.5

    hw = next(r for r in ext.roles if "hardware" in r.title.lower())
    assert "ECE" in hw.eligibility.allowed_branches
    assert "EEE" in hw.eligibility.allowed_branches

    sw = next(r for r in ext.roles if "software" in r.title.lower())
    assert "CSE" in sw.eligibility.allowed_branches
    assert "IT" in sw.eligibility.allowed_branches
