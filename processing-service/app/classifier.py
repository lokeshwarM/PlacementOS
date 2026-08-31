import re
from typing import Tuple, List
from app.models import EmailPayload, ClassificationResult

# High-weight placement phrases
PLACEMENT_STRONG_PATTERNS = [
    r"\bplacement\s+drive\b",
    r"\bcampus\s+recruitment\b",
    r"\brecruitment\s+drive\b",
    r"\bhiring\s+process\b",
    r"\bcampus\s+drive\b",
    r"\bjob\s+description\b",
    r"\bctc\s*:\s*\d+",
    r"\b\d+\s*lpa\b",
    r"\beligible\s+branches\b",
    r"\beligibility\s+criteria\b",
    r"\bminimum\s+cgpa\b",
    r"\bstanding\s+arrears\b",
    r"\bapply\s+before\b",
    r"\bregistration\s+deadline\b",
    r"\broles?\s+offered\b",
    r"\bpositions?\s+available\b",
    r"\bshortlisted\s+candidates\b",
]

# General placement keywords
PLACEMENT_KEYWORD_PATTERNS = [
    r"\bplacement\b",
    r"\brecruitment\b",
    r"\binterview\b",
    r"\bstipend\b",
    r"\bshortlist\b",
    r"\bcdc\b",
    r"\bpat\s+office\b",
    r"\bcareer\s+development\b",
    r"\bcompany\b",
    r"\broles?\b",
    r"\bsalary\b",
    r"\bpackage\b",
]

# Obvious non-placement patterns (clubs, cultural, hostel, semester exams)
NON_PLACEMENT_PATTERNS = [
    r"\bcultural\s+fest\b",
    r"\bdance\s+club\b",
    r"\bmusic\s+club\b",
    r"\bdrama\s+club\b",
    r"\bclub\s+recruitment\b",
    r"\bclub\s+audition\b",
    r"\bstudent\s+chapter\b",
    r"\bsemester\s+fee\b",
    r"\bexamination\s+timetable\b",
    r"\bhall\s+ticket\b",
    r"\bhostel\s+circular\b",
    r"\bmess\s+committee\b",
    r"\battendance\s+shortage\b",
    r"\bsports\s+meet\b",
    r"\bblood\s+donation\b",
    r"\bworkshop\s+registration\b",
]

class EmailClassifier:

    def classify(self, email: EmailPayload) -> ClassificationResult:
        text = f"{email.subject or ''}\n{email.plain_text_body or ''}\n{email.snippet or ''}".lower()

        evidence = []
        positive_score = 0
        negative_score = 0

        # Check negative patterns first
        for pat in NON_PLACEMENT_PATTERNS:
            match = re.search(pat, text)
            if match:
                negative_score += 3
                evidence.append(f"Non-placement pattern matched: '{match.group(0)}'")

        # Check strong placement patterns
        for pat in PLACEMENT_STRONG_PATTERNS:
            match = re.search(pat, text)
            if match:
                positive_score += 3
                evidence.append(f"Strong placement signal: '{match.group(0)}'")

        # Check general placement keywords
        for pat in PLACEMENT_KEYWORD_PATTERNS:
            match = re.search(pat, text)
            if match:
                positive_score += 1

        # Evaluate outcome
        if negative_score >= 3 and positive_score < 3:
            return ClassificationResult(
                is_placement=False,
                confidence=min(0.95, 0.70 + (negative_score * 0.05)),
                evidence=evidence[:4]
            )

        if positive_score >= 3 and negative_score == 0:
            return ClassificationResult(
                is_placement=True,
                confidence=min(0.98, 0.75 + (positive_score * 0.04)),
                evidence=evidence[:4]
            )

        if positive_score > negative_score:
            return ClassificationResult(
                is_placement=True,
                confidence=0.80,
                evidence=evidence[:4]
            )
        else:
            return ClassificationResult(
                is_placement=False,
                confidence=0.85,
                evidence=evidence[:4] if evidence else ["No placement indicators found"]
            )
