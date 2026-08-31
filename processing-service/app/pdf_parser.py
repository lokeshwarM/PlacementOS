import io
import re
from typing import List, Tuple
from pypdf import PdfReader
from app.models import DocumentCandidate

REG_NO_REGEX = re.compile(r"\b(\d{2}[A-Za-z]{3}\d{4,5})\b")
NEOPAT_REGEX = re.compile(r"\b(NP\d{3,8}|NEO\d{3,8})\b", re.IGNORECASE)

def parse_pdf_document(file_bytes: bytes) -> Tuple[List[DocumentCandidate], bool, str]:
    """
    Parses a PDF document, extracts text across pages, and detects candidate rows.
    If extracted text is minimal/empty (<40 characters), returns ocr_required = True.
    """
    candidates: List[DocumentCandidate] = []
    seen_keys = set()
    total_text = ""

    try:
        reader = PdfReader(io.BytesIO(file_bytes))
    except Exception as e:
        return candidates, True, ""

    if len(reader.pages) == 0:
        return candidates, True, ""

    for page_idx, page in enumerate(reader.pages):
        try:
            page_text = page.extract_text() or ""
        except Exception:
            page_text = ""

        total_text += "\n" + page_text

        lines = page_text.splitlines()
        for line_idx, line in enumerate(lines):
            line_str = line.strip()
            if not line_str:
                continue

            reg_match = REG_NO_REGEX.search(line_str)
            neo_match = NEOPAT_REGEX.search(line_str)

            reg_no = reg_match.group(1).upper() if reg_match else None
            neopat = neo_match.group(1).upper() if neo_match else None

            # Attempt to extract candidate name from line by removing reg/neopat and common noise
            name = None
            if reg_no or neopat:
                rem = line_str
                if reg_no:
                    rem = rem.replace(reg_no, " ")
                if neopat:
                    rem = rem.replace(neopat, " ")

                # Clean non-name tokens (digits, punctuation, table pipes)
                cleaned_rem = re.sub(r"[|\t,;:]", " ", rem)
                cleaned_rem = re.sub(r"^\s*\d+\s*", "", cleaned_rem) # remove leading serial numbers
                cleaned_rem = re.sub(r"\s+", " ", cleaned_rem).strip()

                # Filter out pure noise or single character tokens
                name_words = [w for w in cleaned_rem.split(" ") if len(w) > 1 and not w.isdigit()]
                if name_words:
                    name = " ".join(name_words)

            if reg_no or neopat or name:
                key = (reg_no or "", neopat or "", name or "")
                if key in seen_keys or (not reg_no and not neopat and (not name or len(name) < 3)):
                    continue
                seen_keys.add(key)

                evidence = f"PDF Page {page_idx + 1}, Line {line_idx + 1}: {line_str[:120]}"
                candidates.append(DocumentCandidate(
                    registration_number=reg_no,
                    neopat_id=neopat,
                    name=name,
                    role=None,
                    evidence=evidence
                ))

    # If document has almost no extractable text, mark OCR_REQUIRED
    ocr_required = len(total_text.strip()) < 40 and len(candidates) == 0

    return candidates, ocr_required, total_text
