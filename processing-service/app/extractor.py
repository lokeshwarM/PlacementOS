import re
from typing import List, Optional, Tuple, Dict
from datetime import datetime, timezone
from app.models import (
    EmailPayload,
    ExtractionResult,
    ClassificationResult,
    EligibilityModel,
    RoleModel,
    ImportantDateModel
)

MONTH_MAP = {
    "january": 1, "jan": 1,
    "february": 2, "feb": 2,
    "march": 3, "mar": 3,
    "april": 4, "apr": 4,
    "may": 5,
    "june": 6, "jun": 6,
    "july": 7, "jul": 7,
    "august": 8, "aug": 8,
    "september": 9, "sep": 9, "sept": 9,
    "october": 10, "oct": 10,
    "november": 11, "nov": 11,
    "december": 12, "dec": 12
}

BRANCH_PATTERNS = [
    (r"\bcse\b|\bcomputer\s+science\b", "CSE"),
    (r"\bit\b|\binformation\s+technology\b", "IT"),
    (r"\bece\b|\belectronics\s+and\s+communication\b", "ECE"),
    (r"\beee\b|\belectrical\s+and\s+electronics\b", "EEE"),
    (r"\bmech\b|\bmechanical\b", "MECH"),
    (r"\bcivil\b", "CIVIL"),
    (r"\baids\b|\bai\s*&\s*ds\b|\bartificial\s+intelligence\b", "AI&DS"),
    (r"\bcsbs\b", "CSBS"),
]

SPECIALIZATION_PATTERNS = [
    (r"\bdata\s+science\b|\bds\b", "Data Science"),
    (r"\bai\b|\bartificial\s+intelligence\b", "Artificial Intelligence"),
    (r"\binformation\s+security\b|\bcyber\s+security\b", "Information Security"),
    (r"\bcloud\s+computing\b", "Cloud Computing"),
    (r"\biot\b|\binternet\s+of\s+things\b", "IoT"),
]

class PlacementExtractor:

    def extract(self, email: EmailPayload, classification: ClassificationResult) -> ExtractionResult:
        if not classification.is_placement:
            return ExtractionResult(
                message_id=email.message_id,
                source_email=email.source_email,
                classification=classification,
                company_name=None,
                roles=[]
            )

        subject = email.subject or ""
        body = email.plain_text_body or email.snippet or ""
        full_text = f"{subject}\n{body}"

        field_evidence = {}

        # 1. Company Name
        company_name, comp_ev = self._extract_company_name(subject, body)
        if comp_ev:
            field_evidence["companyName"] = comp_ev[:150]

        # 2. Drive Title
        drive_title = subject if subject else (f"{company_name} Placement Drive" if company_name else "Placement Drive")

        # 3. Application Deadline & Important Dates
        deadline, deadline_ev = self._extract_deadline(full_text)
        if deadline_ev:
            field_evidence["applicationDeadline"] = deadline_ev[:150]

        important_dates = self._extract_important_dates(full_text)

        # 4. Extract Roles & Segment Text
        roles, common_text, role_segments = self._extract_roles_and_segments(body, drive_title)

        # 5. Extract Common Eligibility
        common_eligibility, common_ev = self._extract_eligibility(common_text if common_text else full_text)
        if common_ev:
            field_evidence["commonEligibility"] = common_ev[:150]

        # 6. Extract Role-Specific Eligibility
        for role in roles:
            norm_title = role.title.lower()
            segment = role_segments.get(norm_title)
            if segment:
                role_elig, role_ev = self._extract_eligibility(segment, is_role_specific=True)
                if role_elig:
                    role.eligibility = role_elig
                    if role_ev:
                        role.evidence = {"eligibility": role_ev[:150]}

        return ExtractionResult(
            message_id=email.message_id,
            source_email=email.source_email,
            classification=classification,
            company_name=company_name,
            drive_title=drive_title,
            description=body[:500] if body else None,
            application_deadline=deadline,
            important_dates=important_dates,
            common_eligibility=common_eligibility,
            roles=roles,
            field_evidence=field_evidence
        )

    def _extract_company_name(self, subject: str, body: str) -> Tuple[Optional[str], Optional[str]]:
        # Check patterns in subject
        sub_clean = re.sub(r"^(re|fwd|fw):\s*", "", subject, flags=re.IGNORECASE).strip()

        patterns = [
            r"(?:placement\s+drive|campus\s+recruitment(?:\s+drive)?|recruitment\s+drive|campus\s+drive|hiring\s+drive)\s*[-:|]\s*([A-Za-z0-9\s&.+]+)",
            r"([A-Za-z0-9\s&.+]+)\s+(?:is\s+conducting|is\s+visiting|campus\s+drive|recruitment\s+drive|placement\s+drive|hiring)",
            r"(?:company\s*name\s*[:\-]\s*)([A-Za-z0-9\s&.+]+)",
            r"([A-Za-z0-9\s&.+]+)\s*[-|]\s*(?:fte|internship|campus\s+hiring|campus\s+recruitment)",
        ]

        for pat in patterns:
            match = re.search(pat, sub_clean, re.IGNORECASE)
            if match:
                candidate = match.group(1).strip()
                # Exclude generic words
                if len(candidate) > 1 and candidate.lower() not in ["cdc", "vit", "placement", "campus", "drive", "hiring"]:
                    # Clean trailing words
                    candidate = re.split(r"\s+[-|]\s+", candidate)[0].strip()
                    return candidate, f"Extracted from subject: '{match.group(0)}'"

        for pat in patterns:
            match = re.search(pat, body, re.IGNORECASE)
            if match:
                candidate = match.group(1).strip()
                if len(candidate) > 1 and candidate.lower() not in ["cdc", "vit", "placement", "campus", "drive", "hiring"]:
                    candidate = re.split(r"\s+[-|]\s+", candidate)[0].strip()
                    return candidate, f"Extracted from body: '{match.group(0)}'"

        # Fallback: extract first capitalized entity
        words = sub_clean.split()
        if words and words[0].lower() not in ["placement", "campus", "cdc", "recruitment", "hiring", "urgent"]:
            return words[0], f"Derived from subject token: '{words[0]}'"

        return "Unknown Company", None

    def _extract_roles_and_segments(self, text: str, fallback_title: str) -> Tuple[List[RoleModel], str, Dict[str, str]]:
        roles: List[RoleModel] = []
        role_segments: Dict[str, str] = {}
        common_text = text

        # Look for a dedicated Roles section
        role_section_match = re.search(
            r"(?:roles?(?:\s+offered)?|positions?(?:\s+available)?|job\s+roles?)\s*:\s*\n?((?:(?:\d+[\.\)]\s*|[-*]\s*)[^\n]+\n?)+)",
            text,
            re.IGNORECASE
        )

        if role_section_match:
            raw_roles_block = role_section_match.group(1)
            lines = raw_roles_block.strip().split("\n")
            order = 1
            for line in lines:
                cleaned = re.sub(r"^(?:\d+[\.\)]\s*|[-*]\s*)", "", line).strip()
                if cleaned and len(cleaned) > 2:
                    # Clean trailing CTC/details if present on same line
                    role_title = re.split(r"\s*[-–(]\s*(?:ctc|stipend|\d+\s*lpa)", cleaned, flags=re.IGNORECASE)[0].strip()
                    roles.append(RoleModel(title=role_title, role_order=order))
                    order += 1

        # Look for role-specific sections in the text, e.g. "For Data Scientist role: ..." or "Software Engineer:\n..."
        role_specific_matches = re.finditer(
            r"(?:for\s+([A-Za-z\s]+?)\s+role|role\s*:\s*([A-Za-z\s]+?))\s*[:\-]\s*([^\n]+(?:\n[^\n]+){0,4})",
            text,
            re.IGNORECASE
        )
        for m in role_specific_matches:
            r_name = (m.group(1) or m.group(2)).strip().lower()
            segment_text = m.group(3).strip()
            role_segments[r_name] = segment_text
            # If not in roles list, add it
            if not any(r.title.lower() == r_name for r in roles):
                roles.append(RoleModel(title=r_name.title(), role_order=len(roles) + 1))

        if not roles:
            # Single fallback role
            roles.append(RoleModel(title=fallback_title if fallback_title else "Software Engineer", role_order=1))

        return roles, common_text, role_segments

    def _extract_eligibility(self, text: str, is_role_specific: bool = False) -> Tuple[Optional[EligibilityModel], Optional[str]]:
        evidence_parts = []
        has_any = False

        # 1. CGPA
        cgpa_match = re.search(
            r"(?:minimum\s+cgpa|cgpa\s*(?:>=|:|of)?)\s*[:\-]?\s*(\d+(?:\.\d+)?)|\b(\d+\.\d+)\s*(?:cgpa|and\s+above\s+cgpa)",
            text,
            re.IGNORECASE
        )
        min_cgpa = None
        if cgpa_match:
            val_str = cgpa_match.group(1) or cgpa_match.group(2)
            try:
                val = float(val_str)
                if 0.0 <= val <= 10.0:
                    min_cgpa = val
                    has_any = True
                    evidence_parts.append(f"CGPA: {min_cgpa}")
            except ValueError:
                pass

        # 2. Branches (Only check if text contains eligible branches keywords or in role specific)
        allowed_branches = []
        if is_role_specific or re.search(r"\b(eligible\s+branches?|branches?|eligibility|departments?)\b", text, re.IGNORECASE):
            for pat, code in BRANCH_PATTERNS:
                if re.search(pat, text, re.IGNORECASE):
                    if code not in allowed_branches:
                        allowed_branches.append(code)
            if allowed_branches:
                has_any = True
                evidence_parts.append(f"Branches: {', '.join(allowed_branches)}")

        # 3. Specializations
        allowed_specs = []
        for pat, spec_name in SPECIALIZATION_PATTERNS:
            if re.search(pat, text, re.IGNORECASE):
                if spec_name not in allowed_specs:
                    allowed_specs.append(spec_name)
        if allowed_specs:
            has_any = True
            evidence_parts.append(f"Specializations: {', '.join(allowed_specs)}")

        # 4. Batches
        allowed_batches = []
        batch_matches = re.finditer(r"\b(202[4-9])\s*(?:batch|graduating|passout)?\b", text, re.IGNORECASE)
        for bm in batch_matches:
            b_yr = bm.group(1)
            if b_yr not in allowed_batches:
                allowed_batches.append(b_yr)
        if allowed_batches:
            has_any = True
            evidence_parts.append(f"Batches: {', '.join(allowed_batches)}")

        # 5. Arrears
        no_arrears = None
        if re.search(r"\bno\s+(?:standing\s+|active\s+)?arrears?\b|\b0\s+standing\s+arrears?\b|\bno\s+backlogs?\b", text, re.IGNORECASE):
            no_arrears = True
            has_any = True
            evidence_parts.append("No standing arrears required")

        if not has_any:
            return None, None

        elig = EligibilityModel(
            minimum_cgpa=min_cgpa,
            allowed_branches=allowed_branches if allowed_branches else None,
            allowed_specializations=allowed_specs if allowed_specs else None,
            allowed_batches=allowed_batches if allowed_batches else None,
            no_standing_arrears=no_arrears
        )
        return elig, "; ".join(evidence_parts)

    def _extract_deadline(self, text: str) -> Tuple[Optional[str], Optional[str]]:
        deadline_match = re.search(
            r"(?:apply\s+before|registration\s+deadline|application\s+deadline|last\s+date\s+to\s+apply)\s*[:\-]?\s*([^\n]+)",
            text,
            re.IGNORECASE
        )
        if not deadline_match:
            return None, None

        raw_str = deadline_match.group(1).strip()
        parsed_iso = self._parse_date_to_iso(raw_str)
        return parsed_iso, f"Deadline found: '{raw_str[:80]}'"

    def _extract_important_dates(self, text: str) -> List[ImportantDateModel]:
        dates = []
        patterns = [
            (r"(?:online\s+test|test\s+date)\s*[:\-]?\s*([^\n]+)", "Online Assessment"),
            (r"(?:interview\s+date|interview\s+schedule)\s*[:\-]?\s*([^\n]+)", "Interview Schedule"),
            (r"(?:ppt\s+date|pre-placement\s+talk)\s*[:\-]?\s*([^\n]+)", "Pre-Placement Talk")
        ]

        for pat, desc in patterns:
            m = re.search(pat, text, re.IGNORECASE)
            if m:
                raw = m.group(1).strip()
                iso_dt = self._parse_date_to_iso(raw)
                dates.append(ImportantDateModel(description=desc, date=iso_dt, raw_text=raw[:60]))

        return dates

    def _parse_date_to_iso(self, date_str: str) -> Optional[str]:
        # Example formats:
        # "September 3, 2026 at 6:00 PM"
        # "3rd September 2026, 18:00"
        # "2026-09-03 18:00"
        cleaned = re.sub(r"(st|nd|rd|th)", "", date_str, flags=re.IGNORECASE)

        # Match Month Day, Year Time
        m = re.search(
            r"([A-Za-z]+)\s+(\d{1,2}),?\s+(\d{4})(?:\s+(?:at|,)?\s*(\d{1,2}):(\d{2})(?:\s*(am|pm))?)?",
            cleaned,
            re.IGNORECASE
        )
        if m:
            month_name = m.group(1).lower()
            day = int(m.group(2))
            year = int(m.group(3))
            hour = int(m.group(4)) if m.group(4) else 23
            minute = int(m.group(5)) if m.group(5) else 59
            meridiem = m.group(6)

            month = MONTH_MAP.get(month_name)
            if month:
                if meridiem:
                    if meridiem.lower() == "pm" and hour < 12:
                        hour += 12
                    elif meridiem.lower() == "am" and hour == 12:
                        hour = 0
                try:
                    dt = datetime(year, month, day, hour, minute, 0, tzinfo=timezone.utc)
                    return dt.isoformat()
                except ValueError:
                    pass

        # Match Day Month Year
        m2 = re.search(
            r"(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})(?:\s+(?:at|,)?\s*(\d{1,2}):(\d{2})(?:\s*(am|pm))?)?",
            cleaned,
            re.IGNORECASE
        )
        if m2:
            day = int(m2.group(1))
            month_name = m2.group(2).lower()
            year = int(m2.group(3))
            hour = int(m2.group(4)) if m2.group(4) else 23
            minute = int(m2.group(5)) if m2.group(5) else 59
            meridiem = m2.group(6)

            month = MONTH_MAP.get(month_name)
            if month:
                if meridiem:
                    if meridiem.lower() == "pm" and hour < 12:
                        hour += 12
                    elif meridiem.lower() == "am" and hour == 12:
                        hour = 0
                try:
                    dt = datetime(year, month, day, hour, minute, 0, tzinfo=timezone.utc)
                    return dt.isoformat()
                except ValueError:
                    pass

        return None
