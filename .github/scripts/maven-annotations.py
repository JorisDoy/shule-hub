"""Turn a failed Maven build log into GitHub annotations, so errors are readable from the API.

GitHub allows 10 annotations of each level per step, so the relevant lines are packed into a few
long messages instead of one annotation per error.
"""
import re
import sys

BOILERPLATE = (
    "-> [Help 1]", "To see the full stack trace", "Re-run Maven using", "For more information about",
    "[Help 1]", "After correcting the problems", "mvn <args> -rf",
)
CHUNK = 6000

ansi = re.compile(r"\x1b\[[0-9;]*m")
picked, seen = [], set()
with open(sys.argv[1], encoding="utf-8", errors="replace") as log:
    for raw in log:
        line = ansi.sub("", raw).rstrip()
        if not line.strip("[ERROR] ").strip():
            continue
        relevant = (
            line.startswith("[ERROR]")
            or "Caused by:" in line
            or re.search(r"Tests run: \d+, Failures: [1-9]|Errors: [1-9]", line)
            or "APPLICATION FAILED TO START" in line
            or line.startswith("Description:") or line.startswith("Action:")
        )
        if not relevant or any(text in line for text in BOILERPLATE):
            continue
        line = line.replace("/home/runner/work/shule-hub/shule-hub/", "")
        if line not in seen:
            seen.add(line)
            picked.append(line)

if not picked:
    picked = ["No [ERROR] lines found; see the uploaded build log."]

chunks, current = [], ""
for line in picked:
    if len(current) + len(line) + 1 > CHUNK:
        chunks.append(current)
        current = ""
    current += line + "\n"
chunks.append(current)


def escape(text):
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


levels = ["error"] * 10 + ["warning"] * 10 + ["notice"] * 10
for index, (level, chunk) in enumerate(zip(levels, chunks), start=1):
    print(f"::{level} title=Maven build output {index}/{len(chunks)}::{escape(chunk)}")
