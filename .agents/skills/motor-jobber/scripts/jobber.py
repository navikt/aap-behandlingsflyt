#!/usr/bin/env python3
"""Inventory of motor jobs (no.nav.aap.motor) registered by this app, read from the Kotlin sources.

Shows for each JobbSpesifikasjon in ProsesseringsJobber.alle(): the `type` string stored in the
JOBB table, class + file, cron, retries / retryBackoffTid / prioritet overrides, and how call
sites group the job (forBehandling / forSak / none). Grouping is a heuristic based on the code
near each `JobbInput(...)` construction — verify in the source when it matters.

Usage:
  jobber.py                         table of all jobs
  jobber.py --type flyt.prosesserBehandling   details for one job type (or class name)
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ID = r"[A-Za-z_ÆØÅæøå][A-Za-z0-9_ÆØÅæøå]*"

# Built into the motor library itself (registered in no.nav.aap.motor.JobbType). Both are cron jobs, so
# RetryService.enable() (called at app startup) plans them like the app's own cron jobs.
BUILT_IN = [
    ("oppgave.retryFeilede", "RekjørFeiledeJobb (motor)", "0 0 7,12,15,20 * * *",
     "Sets ALL FEILET jobs back to KLAR (each gets one more attempt)."),
    ("jobber.arkiverFerdigstilte", "ArkiverFerdigstilteJobb (motor)", "0 0 1 * * *",
     "Moves FERDIG jobs to jobb_arkiv / jobb_historikk_arkiv."),
]


def repo_root() -> Path:
    for p in Path(__file__).resolve().parents:
        if (p / "settings.gradle.kts").exists() and (p / "behandlingsflyt").is_dir():
            return p
    sys.exit("Could not locate repository root")


ROOT = repo_root()
MAIN = ROOT / "behandlingsflyt/src/main/kotlin"
REGISTRY = MAIN / "no/nav/aap/behandlingsflyt/prosessering/ProsesseringsJobber.kt"


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8")


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?m)^\s*//.*$|(?<=[^:\"])//[^\"\n]*$", "", src)


class Jobb:
    def __init__(self, klass: str, kun_ikke_prod: bool):
        self.klass, self.kun_ikke_prod = klass, kun_ikke_prod
        self.file: Optional[Path] = None
        self.type = self.navn = self.beskrivelse = self.cron = self.retries = self.backoff = self.prioritet = ""
        self.grouping: Dict[str, int] = {}


def statement_at(src: str, start: int) -> str:
    """Text of the expression starting at `start`, following `.chain()` continuations across lines."""
    depth, i = 0, start
    while i < len(src):
        c = src[i]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
            if depth < 0:
                break
        elif c == "\n" and depth == 0:
            rest = src[i + 1:].lstrip()
            if not rest.startswith("."):
                break
        i += 1
    return src[start:i]


def grouping_of(chain: str) -> str:
    if "forBehandling(" in chain:
        return "forBehandling"
    if "forSak(" in chain:
        return "forSak"
    return "none (selvstendig)"


def load() -> List[Jobb]:
    reg = strip_comments(read(REGISTRY))
    body = reg[reg.index("listOfNotNull("):]
    jobs = []
    for m in re.finditer(r"(if\s*\(!Miljø\.erProd\(\)\)\s*)?\b(" + ID + r")\b\s*(?:else\s+null)?\s*,?", body):
        name = m.group(2)
        if name[0].isupper() and name not in ("Miljø",) and (name.endswith("Utfører") or name.endswith("UtFører")):
            jobs.append(Jobb(name, bool(m.group(1))))
    files = {}
    for f in ROOT.glob("behandlingsflyt/src/main/kotlin/**/*.kt"):
        src = read(f)
        if "JobbSpesifikasjon" in src or "JobbInput" in src:
            files[f] = strip_comments(src)
    for j in jobs:
        nyjobb_grouping = None
        for f, src in files.items():
            cm = re.search(r"\b(class|object)\s+" + j.klass + r"\b", src)
            if not cm:
                continue
            j.file = f
            spec = src[cm.start():] if cm.group(1) == "object" else src[src.find("companion object", cm.start()):]

            def grab(pattern: str) -> str:
                mm = re.search(pattern, spec)
                return mm.group(1) if mm else ""
            j.type = grab(r"override\s+val\s+type\s*(?::\s*String)?\s*=\s*\"([^\"]+)\"")
            j.navn = grab(r"override\s+val\s+navn\s*(?::\s*String)?\s*=\s*\"([^\"]+)\"")
            j.beskrivelse = grab(r"override\s+val\s+beskrivelse\s*(?::\s*String)?\s*=\s*\"+([^\"]+)\"")
            j.cron = grab(r"override\s+val\s+cron[^=]*=\s*CronExpression\.\w+\(\s*\"([^\"]+)\"")
            j.retries = grab(r"override\s+val\s+retries\s*(?::\s*Int)?\s*=\s*(\d+)")
            j.backoff = grab(r"override\s+val\s+retryBackoffTid[^=]*=\s*([^\n]+)").strip()
            j.prioritet = grab(r"override\s+val\s+prioritet[^=]*=\s*([^\n]+)").strip()
            ny = re.search(r"fun\s+nyJobb\s*\(", spec)
            if ny:
                nb = spec[ny.start():]
                eq = re.search(r"\)\s*(?::\s*JobbInput\s*)?(=|\{)", nb)
                if eq and eq.group(1) == "=":
                    start = eq.end() + (len(nb[eq.end():]) - len(nb[eq.end():].lstrip()))
                    nyjobb_grouping = grouping_of(statement_at(nb, start))
                else:
                    nyjobb_grouping = grouping_of(nb[:1500])
        for f, src in files.items():
            for mm in re.finditer(r"JobbInput\(\s*(?:jobb\s*=\s*)?" + j.klass + r"\s*\)|\b" + j.klass + r"\.nyJobb\(", src):
                if mm.group(0).endswith("nyJobb(") and nyjobb_grouping:
                    key = nyjobb_grouping
                else:
                    key = grouping_of(statement_at(src, mm.start()))
                j.grouping[key] = j.grouping.get(key, 0) + 1
    return jobs


def fmt_group(j: Jobb) -> str:
    if not j.grouping:
        return "cron-planned (selvstendig)" if j.cron else "no JobbInput call site found"
    return ", ".join(f"{k}×{v}" for k, v in sorted(j.grouping.items()))


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--type", help="job type string or class name")
    a = ap.parse_args()
    jobs = load()
    if a.type:
        q = a.type.lower()
        for t, k, cron, desc in BUILT_IN:
            if q in (t.lower(), k.lower().split()[0]):
                print(f"{t}: {k}, cron {cron}. {desc}")
                return
        hits = [j for j in jobs if q in (j.type.lower(), j.klass.lower())]
        if not hits:
            sys.exit("No such job")
        for j in hits:
            print(f"type        : {j.type}")
            print(f"class       : {j.klass}  {j.file.relative_to(ROOT) if j.file else '?'}")
            print(f"navn        : {j.navn}")
            print(f"beskrivelse : {j.beskrivelse}")
            print(f"cron        : {j.cron or '— (enqueued by code)'}")
            print(f"retries     : {j.retries or '3 (default)'}   retryBackoffTid: {j.backoff or 'none (retries immediately)'}")
            print(f"prioritet   : {j.prioritet or 'Prioritet.NORMAL (100, default; call sites may override with medPrioritet)'}")
            print(f"grouping    : {fmt_group(j)}   (exclusivity group = sak_id, behandling_id, type)")
            if j.kun_ikke_prod:
                print("NOTE        : only registered outside prod (if (!Miljø.erProd()))")
        return
    print(f"{'type':48} {'class':52} {'cron':22} {'retries':8} grouping (heuristic)")
    for j in jobs:
        extra = (" [ikke prod]" if j.kun_ikke_prod else "")
        retr = (j.retries or "3") + ("+bo" if j.backoff else "")
        print(f"{j.type:48} {j.klass + extra:52} {j.cron or '-':22} {retr:8} {fmt_group(j)}")
    for t, k, cron, _ in BUILT_IN:
        print(f"{t:48} {k:52} {cron:22} {'3':8} none (selvstendig)")
    print("\nretries '+bo' = has retryBackoffTid. Details: --type <type|Class>")


if __name__ == "__main__":
    main()
