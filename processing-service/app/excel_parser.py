import io
import re
import openpyxl
from typing import List, Optional, Tuple, Dict
from app.models import DocumentCandidate

REG_NO_PATTERNS = [
    r"^reg(\.?\s*|\s*_)no(\.?)?$",
    r"^registration\s*no(\.?)?$",
    r"^registration\s*number$",
    r"^regno$",
    r"^roll\s*no(\.?)?$",
    r"^roll\s*number$",
    r"^student\s*id$"
]

NEOPAT_PATTERNS = [
    r"^neopat\s*id$",
    r"^neopat$",
    r"^neo\s*pat$",
    r"^neopat_id$",
    r"^neopatid$"
]

NAME_PATTERNS = [
    r"^name$",
    r"^candidate\s*name$",
    r"^student\s*name$",
    r"^name\s*of\s*(the\s*)?candidate$",
    r"^applicant\s*name$",
    r"^full\s*name$"
]

ROLE_PATTERNS = [
    r"^role$",
    r"^position$",
    r"^job\s*role$",
    r"^applied\s*role$",
    r"^profile$"
]

def parse_excel_document(file_bytes: bytes) -> List[DocumentCandidate]:
    """
    Parses an Excel (.xlsx, .xls) workbook, finds header columns across sheets,
    and extracts structured shortlist candidates.
    """
    candidates: List[DocumentCandidate] = []
    seen_keys = set()

    try:
        wb = openpyxl.load_workbook(io.BytesIO(file_bytes), data_only=True, read_only=True)
    except Exception as e:
        return candidates

    for sheet_name in wb.sheetnames:
        sheet = wb[sheet_name]
        rows = list(sheet.iter_rows(values_only=True))
        if not rows:
            continue

        # Find header row
        header_row_idx, col_map = find_header_row(rows)
        if header_row_idx is None:
            # Fallback: try value pattern detection across entire sheet
            col_map = detect_columns_by_values(rows)
            header_row_idx = 0

        # Extract rows starting after header
        for row_idx in range(header_row_idx + 1, len(rows)):
            row = rows[row_idx]
            if not row or not any(row):
                continue

            reg_no = get_cell_val(row, col_map.get("reg_no"))
            neopat = get_cell_val(row, col_map.get("neopat"))
            name = get_cell_val(row, col_map.get("name"))
            role = get_cell_val(row, col_map.get("role"))

            if not reg_no and not neopat and not name:
                continue

            # Clean and validate
            if reg_no:
                reg_no = reg_no.strip()
                # Check for standard VIT-like or alphanumeric ID
                if len(reg_no) < 4:
                    reg_no = None

            if neopat:
                neopat = neopat.strip()

            if name:
                name = name.strip()

            if not reg_no and not neopat and not name:
                continue

            key = (reg_no or "", neopat or "", name or "")
            if key in seen_keys:
                continue
            seen_keys.add(key)

            evidence_parts = [f"Sheet: {sheet_name}", f"Row {row_idx + 1}"]
            if reg_no: evidence_parts.append(f"Reg: {reg_no}")
            if neopat: evidence_parts.append(f"NeoPAT: {neopat}")
            if name: evidence_parts.append(f"Name: {name}")
            if role: evidence_parts.append(f"Role: {role}")

            candidates.append(DocumentCandidate(
                registration_number=reg_no,
                neopat_id=neopat,
                name=name,
                role=role,
                evidence=" | ".join(evidence_parts)
            ))

    return candidates

def find_header_row(rows: List[tuple]) -> Tuple[Optional[int], Dict[str, int]]:
    for idx in range(min(20, len(rows))):
        row = rows[idx]
        if not row:
            continue
        col_map = {}
        for c_idx, cell in enumerate(row):
            if cell is None:
                continue
            c_str = str(cell).strip().lower()
            if not c_str:
                continue

            if matches_any(c_str, REG_NO_PATTERNS) and "reg_no" not in col_map:
                col_map["reg_no"] = c_idx
            elif matches_any(c_str, NEOPAT_PATTERNS) and "neopat" not in col_map:
                col_map["neopat"] = c_idx
            elif matches_any(c_str, NAME_PATTERNS) and "name" not in col_map:
                col_map["name"] = c_idx
            elif matches_any(c_str, ROLE_PATTERNS) and "role" not in col_map:
                col_map["role"] = c_idx

        # If at least one candidate identifier column is detected, accept as header row
        if "reg_no" in col_map or "neopat" in col_map or "name" in col_map:
            return idx, col_map

    return None, {}

def detect_columns_by_values(rows: List[tuple]) -> Dict[str, int]:
    col_map = {}
    reg_regex = re.compile(r"^\d{2}[A-Za-z]{3}\d{4,5}$")
    for row in rows[:50]:
        if not row:
            continue
        for c_idx, cell in enumerate(row):
            if cell is None:
                continue
            c_str = str(cell).strip()
            if reg_regex.match(c_str) and "reg_no" not in col_map:
                col_map["reg_no"] = c_idx
    return col_map

def matches_any(text: str, patterns: List[str]) -> bool:
    for pat in patterns:
        if re.search(pat, text):
            return True
    return False

def get_cell_val(row: tuple, col_idx: Optional[int]) -> Optional[str]:
    if col_idx is None or col_idx >= len(row):
        return None
    val = row[col_idx]
    if val is None:
        return None
    s = str(val).strip()
    return s if s else None
