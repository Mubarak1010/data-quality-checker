# Data Quality Checker

> A standalone desktop application for automated CSV data quality validation — built during NYSC service at First Bank of Nigeria HQ, Enterprise Data Management & Transformational Analytics.

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/Java%20Swing-GUI-4A90D9?style=flat-square)
![Status](https://img.shields.io/badge/Status-Complete-27AE60?style=flat-square)
![Version](https://img.shields.io/badge/Version-1.1-C9A84C?style=flat-square)

---

## The Problem

The EDM department at First Bank regularly performs data quality checks across files received in varying formats. The manual effort required to review large datasets for discrepancies significantly increases staff workload beyond normal capacity, while still falling short of guaranteed accuracy. Issues such as missing values, incorrect formats, duplicate records, and structural inconsistencies are common but difficult to catch consistently through manual review. Additionally, when data is shared with third-party vendors, inaccuracies can lead to increased operational costs and reputational risk.

## The Solution

A three-stage desktop application that lets any staff member — technical or not — upload a CSV, define validation rules per column through a simple GUI, and get a colour-coded results report in seconds. No code. No database access. No setup beyond running the JAR.

---

## Features

- **3-Stage GUI** — Upload → Schema Builder → Results & Export
- **5 Built-in Validators** — Not Empty, Numeric, Email, Date, Length
- **Custom Rule Engine** — 17 rule types including Regex, Range checks, Must Be One Of, Starts/Ends With, Case checks, and more
- **Duplicate Detection** — flags exact duplicate rows across the entire dataset
- **Date Normalisation** — automatically converts dates to a chosen format on clean rows
- **Live Progress Tracking** — SwingWorker background thread streams results in real time
- **Colour-coded Results Table** — green (clean), red (failed), amber (duplicate)
- **Three Export Options** — Full report CSV, Clean rows only, Dirty rows only
- **Select All** — toggle validators across all columns at once
- **Persistent Custom Rules** — saved rules survive between sessions

---

## Architecture

```
DataQualityCheckerGUI.java   ← Main application (GUI, stages, validation, export)
CSVFile.java                 ← CSV parsing & structural validation
Validator.java               ← Interface: boolean validate(String value)
EmailValidator.java          ← Regex-based email format check
NumericValidator.java        ← Double.parseDouble numeric check
NotEmptyValidator.java       ← Null/blank/placeholder detection
DateValidator.java           ← Multi-format date validation & conversion
LengthValidator.java         ← Exact string length check
custom_rules.txt             ← Auto-generated persistent custom rules store
```

**Design patterns used:** Interface/Strategy, CardLayout, Observer (SwingWorker), OOP separation of concerns.

---

## How to Run

**Requirements:** Java 8 or later

```bash
# Option 1 — Run the JAR directly
java -jar DataQualityChecker.jar

# Option 2 — Compile and run from source
javac *.java
java DataQualityCheckerGUI
```

---

## Stages

### Stage 1 — Upload
Select a CSV file. The app reads the header, counts rows, and removes any structurally malformed rows (wrong column count) before loading.

### Stage 2 — Schema Builder
A table displays one row per CSV column. Tick the validators you want to apply. Leave a row blank to skip that column. Use the "All" checkbox at the top of each column to select that validator for every column at once.

### Stage 3 — Results & Export
Validation runs in a background thread. Results stream into a colour-coded table live. Summary cards show Total Rows, Clean Rows, Bad Rows, Duplicates, and Pass Rate. Export what you need.

---

## Validators

| Validator | Class | Notes |
|---|---|---|
| Not Empty | `NotEmptyValidator` | Treats n/a, null, none, undefined, -, unknown, ? as empty |
| Numeric | `NumericValidator` | Uses Double.parseDouble — pair with Not Empty for full coverage |
| Email | `EmailValidator` | Regex: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$` |
| Date | `DateValidator` | Accepts 4 formats — also normalises on export |
| Length | `LengthValidator` | User specifies exact expected length per column |
| Custom | `CustomRuleManager` | 17 rule types, persistent, reusable across sessions |

---

## Known Limitations

- CSV values containing commas inside quotes may parse incorrectly (RFC 4180 edge case)
- Standalone JAR requires Java on each machine — use `jpackage` for a bundled `.exe`
- Custom rules stored locally — not suited for multi-user networked environments as-is

---

## Future Roadmap

- [ ] AI-powered anomaly detection (statistical outlier flagging beyond rule-based checks)
- [ ] Auto schema suggestion (AI analyses columns and proposes validators)
- [ ] Natural language rule input ("account numbers must start with 3 and be 10 digits")
- [ ] Spring Boot web version for intranet deployment
- [ ] Power BI integration for live validation dashboards
- [ ] Scheduled / automated validation runs

---

## Author

**Abdulmaleek Mubarak**
BSc Computer Science (First Class) — Ahmadu Bello University
Corp Member, NYSC — First Bank of Nigeria HQ, Enterprise Data Management

[![GitHub](https://img.shields.io/badge/GitHub-Mubarak1010-181717?style=flat-square&logo=github)](https://github.com/Mubarak1010)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Abdulmaleek%20Mubarak-0A66C2?style=flat-square&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/mubarak-abdulmaleek)

---

<sub>Built independently during NYSC service · First Bank of Nigeria · Enterprise Data Management · 2025–2026</sub>
