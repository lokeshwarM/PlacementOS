import re
from typing import Tuple

SHORTLIST_SIGNALS = [
    r"shortlist",
    r"shortlisted",
    r"selected\s+candidates?",
    r"candidates?\s+selected",
    r"selection\s+list",
    r"interview\s+list",
    r"round\s+[1-9]\s+shortlist",
    r"final\s+selects?",
    r"shortlisted\s+students?"
]

JD_SIGNALS = [
    r"job\s+description",
    r"roles?\s+(and|&)\s+responsibilities",
    r"about\s+the\s+role",
    r"key\s+skills?",
    r"job\s+profile",
    r"compensation\s+details"
]

ELIGIBILITY_SIGNALS = [
    r"eligible\s+students?",
    r"eligible\s+candidates?",
    r"eligibility\s+list",
    r"list\s+of\s+eligible"
]

PROCESS_SIGNALS = [
    r"recruitment\s+schedule",
    r"process\s+flow",
    r"interview\s+slots?",
    r"test\s+instructions?",
    r"test\s+link"
]

def classify_document(filename: str, sample_text: str = "") -> Tuple[str, float]:
    """
    Deterministically classifies document intent into:
    - SHORTLIST
    - ELIGIBILITY_DOCUMENT
    - JOB_DESCRIPTION
    - RECRUITMENT_PROCESS
    - OTHER
    """
    fn_raw = filename.lower() if filename else ""
    text_raw = sample_text.lower() if sample_text else ""

    # Replace punctuation / underscores with spaces for clean token matching
    fn_clean = re.sub(r"[_\-\.]+", " ", fn_raw)
    text_clean = re.sub(r"[_\-\.]+", " ", text_raw)

    # 1. Shortlist detection
    for pattern in SHORTLIST_SIGNALS:
        if re.search(pattern, fn_clean):
            return "SHORTLIST", 0.98
        if re.search(pattern, text_clean):
            return "SHORTLIST", 0.90

    # 2. Eligibility Document detection
    for pattern in ELIGIBILITY_SIGNALS:
        if re.search(pattern, fn_clean):
            return "ELIGIBILITY_DOCUMENT", 0.95
        if re.search(pattern, text_clean):
            return "ELIGIBILITY_DOCUMENT", 0.88

    # 3. Job Description detection
    for pattern in JD_SIGNALS:
        if re.search(pattern, fn_clean):
            return "JOB_DESCRIPTION", 0.95
        if re.search(pattern, text_clean):
            return "JOB_DESCRIPTION", 0.88

    # 4. Process detection
    for pattern in PROCESS_SIGNALS:
        if re.search(pattern, fn_clean):
            return "RECRUITMENT_PROCESS", 0.92
        if re.search(pattern, text_clean):
            return "RECRUITMENT_PROCESS", 0.85

    # 5. Default fallback
    return "OTHER", 0.50
