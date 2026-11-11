#!/usr/bin/env python3
"""Repository and Flyway helpers for aap-behandlingsflyt (reads sources, no build needed).

Usage:
  repo.py neste-migrering [--navn kort_beskrivelse]   next free Flyway version (+ suggested file name)
  repo.py oversikt [--filter Sykdom]                  repository interfaces → impl, registrations, kopier/slett
  repo.py sjekk NAME                                  wiring check for one repository interface or impl

`oversikt` columns:
  PG   = impl registered in repository/.../PostgresRepositoryRegistry.kt (`postgresRepositoryRegistry`)
  MEM  = an InMemory* implementation is registered in lib-test InMemoryRepositoryRegistry.kt
  kopier/slett = "noop" when the impl body has no statements (data is NOT copied to revurderinger / NOT deleted);
                 "n/a" when the interface extends the plain komponenter Repository (GrunnlagKopierer skips it)
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


def repo_root() -> Path:
    for p in Path(__file__).resolve().parents:
        if (p / "settings.gradle.kts").exists() and (p / "behandlingsflyt").is_dir():
            return p
    sys.exit("Could not locate repository root")


ROOT = repo_root()
FLYWAY = ROOT / "repository/src/main/resources/flyway"
PG_REGISTRY = ROOT / "repository/src/main/kotlin/no/nav/aap/behandlingsflyt/repository/PostgresRepositoryRegistry.kt"
MEM_REGISTRY = ROOT / "lib-test/src/main/kotlin/no/nav/aap/behandlingsflyt/test/inmemoryrepo/InMemoryRepositoryRegistry.kt"


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8")


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?m)//.*$", "", src)


def body_of(src: str, fun: str) -> Optional[str]:
    m = re.search(r"override\s+fun\s+" + fun + r"\s*\([^)]*\)\s*(?::\s*Unit\s*)?(\{|=)", src)
    if not m:
        return None
    if m.group(1) == "=":
        return src[m.end():src.find("\n", m.end())]
    depth, i = 1, m.end()
    while i < len(src) and depth:
        depth += {"{": 1, "}": -1}.get(src[i], 0)
        i += 1
    return src[m.end():i - 1]


def cmd_neste(navn: Optional[str]) -> None:
    versions: Dict[int, List[str]] = {}
    other = []
    for f in FLYWAY.iterdir():
        m = re.match(r"V1\.(\d+)__.+\.sql$", f.name)
        if m:
            versions.setdefault(int(m.group(1)), []).append(f.name)
        else:
            other.append(f.name)
    hi = max(versions)
    print(f"Highest: V1.{hi}  ({versions[hi][0]})")
    dup = {v: n for v, n in versions.items() if len(n) > 1}
    if dup:
        print(f"WARNING duplicate versions: {dup}")
    if other:
        print(f"Files not matching V1.<n>__*.sql: {other}")
    slug = re.sub(r"[^a-z0-9_]+", "_", (navn or "beskrivelse").lower()).strip("_")
    print(f"Next   : {FLYWAY.relative_to(ROOT)}/V1.{hi + 1}__{slug}.sql")
    print("Note: Flyway orders numerically (V1.9 < V1.10 < V1.357). Never edit a merged migration; add a new one.")


class Repo:
    def __init__(self, iface: str, file: Path):
        self.iface, self.file = iface, file
        self.impl: Optional[str] = None
        self.impl_file: Optional[Path] = None
        self.pg = self.mem = False
        self.kopier = self.slett = "?"


def load() -> List[Repo]:
    repos: Dict[str, Repo] = {}
    for f in (ROOT / "behandlingsflyt/src/main/kotlin").rglob("*.kt"):
        src = strip_comments(read(f))
        for m in re.finditer(r"\binterface\s+(" + ID + r")\s*(?:<[^>]*>)?\s*:\s*([^{\n]+)", src):
            if re.search(r"\bRepository\b", m.group(2)) and "Factory" not in m.group(2) and m.group(1) != "Repository":
                repos[m.group(1)] = Repo(m.group(1), f)
    impl_files = list((ROOT / "repository/src/main/kotlin").rglob("*.kt")) + list(
        (ROOT / "behandlingsflyt/src/main/kotlin").rglob("*.kt"))
    pg = strip_comments(read(PG_REGISTRY))
    mem = strip_comments(read(MEM_REGISTRY)) if MEM_REGISTRY.exists() else ""
    mem_files = list((ROOT / "lib-test/src/main/kotlin").rglob("InMemory*.kt")) + list(
        (ROOT / "lib-test/src/main/kotlin").rglob("ImMemory*.kt"))
    for f in impl_files:
        src = strip_comments(read(f))
        for m in re.finditer(r"\bclass\s+(" + ID + r")\s*(?:\([^)]*\))?\s*:\s*([^{]+)\{", src, re.S):
            supers = re.findall(ID, m.group(2))
            for s in supers:
                if s in repos and repos[s].impl is None and not m.group(1).startswith("InMemory"):
                    r = repos[s]
                    r.impl, r.impl_file = m.group(1), f
                    r.pg = f"register<{m.group(1)}>()" in pg
                    for fun in ("kopier", "slett"):
                        b = body_of(src[m.start():], fun)
                        val = "n/a" if b is None else ("noop" if not b.strip() else "impl")
                        setattr(r, fun, val)
    for f in mem_files:
        src = strip_comments(read(f))
        for m in re.finditer(r"\b(?:class|object)\s+(" + ID + r")\s*(?:\([^)]*\))?\s*:\s*([^{]+)\{", src, re.S):
            for s in re.findall(ID, m.group(2)):
                if s in repos and f"register<{m.group(1)}>()" in mem:
                    repos[s].mem = True
    return sorted(repos.values(), key=lambda r: r.iface)


def rel(p: Optional[Path]) -> str:
    return str(p.relative_to(ROOT)) if p else "—"


def cmd_oversikt(flt: Optional[str]) -> None:
    rows = [r for r in load() if not flt or flt.lower() in r.iface.lower() or flt.lower() in (r.impl or "").lower()]
    print(f"{'Interface':52} {'Impl':54} {'PG':3} {'MEM':4} {'kopier':7} slett")
    for r in rows:
        print(f"{r.iface:52} {(r.impl or '—'):54} {'x' if r.pg else '-':3} {'x' if r.mem else '-':4} {r.kopier:7} {r.slett}")
    missing = [r.iface for r in rows if r.impl and not r.pg]
    if missing:
        print(f"\nImpl exists but not in postgresRepositoryRegistry (may be intentional, e.g. registered elsewhere): {missing}")


def cmd_sjekk(name: str) -> None:
    hits = [r for r in load() if name.lower() in (r.iface.lower(), (r.impl or "").lower())]
    if not hits:
        sys.exit(f"No repository interface/impl named {name}")
    for r in hits:
        print(f"Interface : {r.iface}  {rel(r.file)}")
        print(f"Impl      : {r.impl or '—'}  {rel(r.impl_file)}")
        print(f"Registered in postgresRepositoryRegistry : {r.pg}")
        print(f"InMemory variant registered (lib-test)  : {r.mem}")
        print(f"kopier    : {r.kopier}   slett: {r.slett}")
        if r.kopier == "noop":
            print("  → data in this repository is NOT carried over to a new revurdering")
        if r.kopier == "n/a":
            print("  → extends komponenter Repository directly: no kopier/slett; GrunnlagKopierer skips it")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd")
    n = sub.add_parser("neste-migrering")
    n.add_argument("--navn")
    o = sub.add_parser("oversikt")
    o.add_argument("--filter")
    s = sub.add_parser("sjekk")
    s.add_argument("name")
    a = ap.parse_args()
    if a.cmd == "neste-migrering":
        cmd_neste(a.navn)
    elif a.cmd == "oversikt":
        cmd_oversikt(a.filter)
    elif a.cmd == "sjekk":
        cmd_sjekk(a.name)
    else:
        ap.print_help()


if __name__ == "__main__":
    main()
