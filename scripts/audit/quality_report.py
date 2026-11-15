#!/usr/bin/env python3
"""Build a JSON quality and 90-day churn report for Kotlin source files."""

from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIR = ROOT / "app" / "src" / "main" / "java"
HIGH_CHURN_THRESHOLD = 3
COMPLEXITY_THRESHOLD = 10


def run(command: list[str], *, cwd: Path = ROOT) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        cwd=cwd,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )


def git_churn() -> Counter[str]:
    result = run(
        [
            "git",
            "log",
            "--since=90 days ago",
            "--format=",
            "--name-only",
            "--",
            "app/src/main/java",
        ]
    )
    return Counter(
        line.strip().replace("\\", "/").removeprefix("./")
        for line in result.stdout.splitlines()
        if line.strip()
    )


def classify(max_cc: int, churn: int) -> str:
    if max_cc > COMPLEXITY_THRESHOLD:
        return "HighRisk" if churn >= HIGH_CHURN_THRESHOLD else "SleepingDanger"
    return "ActiveDevelopment" if churn >= HIGH_CHURN_THRESHOLD else "StableCode"


def collect_functions() -> tuple[list[dict[str, object]], dict[str, object]]:
    command = [
        sys.executable,
        "-m",
        "lizard",
        "--csv",
        "-l",
        "kotlin",
        str(SOURCE_DIR),
    ]
    result = run(command)
    functions: list[dict[str, object]] = []
    by_file: dict[str, list[dict[str, object]]] = defaultdict(list)

    for row in csv.reader(result.stdout.splitlines()):
        if not row:
            continue
        if len(row) < 11:
            raise ValueError(f"Unexpected Lizard CSV row ({len(row)} columns): {row!r}")

        nloc, ccn, token_count, parameter_count, length = map(int, row[:5])
        path = row[6].replace("\\", "/")
        if path.startswith(str(ROOT).replace("\\", "/") + "/"):
            path = path[len(str(ROOT).replace("\\", "/")) + 1 :]
        path = path.removeprefix("./")
        function = {
            "path": path,
            "name": row[7],
            "nloc": nloc,
            "cc": ccn,
            "token_count": token_count,
            "parameter_count": parameter_count,
            "length": length,
            "start_line": int(row[9]),
            "end_line": int(row[10]),
        }
        functions.append(function)
        by_file[path].append(function)

    if not functions:
        raise ValueError(f"Lizard found no Kotlin functions under {SOURCE_DIR}")

    global_result = run([sys.executable, "-m", "lizard", "-l", "kotlin", str(SOURCE_DIR)])
    global_match = re.search(
        r"(?m)^\s*(\d+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+(\d+)\s+(\d+)\s+([\d.]+)\s+([\d.]+)\s*$",
        global_result.stdout,
    )
    if not global_match:
        raise ValueError("Could not parse Lizard's global summary row")
    global_summary = {
        "total_nloc": int(global_match.group(1)),
        "average_function_nloc": float(global_match.group(2)),
        "average_cc": float(global_match.group(3)),
        "average_token_count": float(global_match.group(4)),
        "function_count": int(global_match.group(5)),
        "warning_count": int(global_match.group(6)),
    }

    churn = git_churn()
    source_files = sorted(
        path.relative_to(ROOT).as_posix()
        for path in SOURCE_DIR.rglob("*.kt")
        if "build" not in path.parts
    )
    files = []
    for path in source_files:
        file_functions = by_file.get(path, [])
        max_cc = max((int(function["cc"]) for function in file_functions), default=0)
        churn_count = churn[path]
        files.append(
            {
                "path": path,
                "function_count": len(file_functions),
                "max_cc": max_cc,
                "churn_commits_90d": churn_count,
                "risk": classify(max_cc, churn_count),
            }
        )

    summary = {
        "source_file_count": len(source_files),
        "function_count": len(functions),
        "total_function_nloc": sum(int(function["nloc"]) for function in functions),
        "average_cc": round(
            sum(int(function["cc"]) for function in functions) / len(functions), 2
        ),
        "maximum_cc": max(int(function["cc"]) for function in functions),
        "functions_over_cc_threshold": sum(
            int(function["cc"]) > COMPLEXITY_THRESHOLD for function in functions
        ),
        "complexity_threshold_exclusive": COMPLEXITY_THRESHOLD,
        "high_churn_threshold_inclusive": HIGH_CHURN_THRESHOLD,
        "churn_window_days": 90,
    }
    return files, {
        "summary": summary,
        "lizard_global_summary": global_summary,
        "functions": functions,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output",
        type=Path,
        default=ROOT / "build" / "reports" / "quality" / "hotspots.json",
        help="destination JSON path (default: build/reports/quality/hotspots.json)",
    )
    args = parser.parse_args()

    version = run([sys.executable, "-m", "lizard", "--version"]).stdout.strip()
    files, details = collect_functions()
    report = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "analyzer": {"name": "Lizard", "version": version, "language": "Kotlin"},
        **details,
        "risk_matrix": files,
        "threshold_policy": {
            "HighRisk": "max_cc > 10 and churn_commits_90d >= 3",
            "SleepingDanger": "max_cc > 10 and churn_commits_90d < 3",
            "ActiveDevelopment": "max_cc <= 10 and churn_commits_90d >= 3",
            "StableCode": "max_cc <= 10 and churn_commits_90d < 3",
        },
        "limitations": [
            "Churn counts file appearances in commits from the last 90 days, not lines changed.",
            "Lizard complexity is per function; file complexity is the maximum function CC.",
            "This report does not claim SonarQube CK or MOOD measurements.",
        ],
    }

    output = args.output if args.output.is_absolute() else ROOT / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        f"Lizard {version}: {details['summary']['function_count']} functions, "
        f"max CC {details['summary']['maximum_cc']}"
    )
    print(f"Git churn window: 90 days; files classified: {len(files)}")
    print(f"JSON report: {output}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, subprocess.CalledProcessError, ValueError) as error:
        print(f"quality_report: {error}", file=sys.stderr)
        raise SystemExit(1)
