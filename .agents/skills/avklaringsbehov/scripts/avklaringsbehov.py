#!/usr/bin/env python3
"""Look up avklaringsbehov definitions and their wiring straight from the Kotlin sources.

For every `Definisjon` (kontrakt) it shows: kode, BehovType, løsesISteg, totrinn, kvalitetssikres,
roles (løsesAv), the Løsning class (matched via @JsonTypeName), the Løser it delegates to, and
which BehandlingSteg classes reference the definisjon (= where it is created/reconciled).

Usage (run from anywhere inside the repo):
  avklaringsbehov.py                       compact table of all (non-deprecated) definisjoner
  avklaringsbehov.py --alle                include @Deprecated definisjoner
  avklaringsbehov.py --definisjon AVKLAR_SYKDOM   (or --kode 5003) full details
  avklaringsbehov.py --steg AVKLAR_SYKDOM  definisjoner with løsesISteg = that StegType
  avklaringsbehov.py --markdown            reference document (references/definisjoner.md)
  avklaringsbehov.py --check FILE          exit 1 if FILE differs from freshly generated --markdown
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ID = r"[A-Za-z_ÆØÅæøå][A-Za-z0-9_ÆØÅæøå]*"


def repo_root() -> Path:
    for p in Path(__file__).resolve().parents:
        if (p / "settings.gradle.kts").exists() and (p / "behandlingsflyt").is_dir():
            return p
    sys.exit("Could not locate repository root")


ROOT = repo_root()
KONTRAKT = ROOT / "kontrakt/src/main/kotlin/no/nav/aap/behandlingsflyt/kontrakt/avklaringsbehov"
MAIN = ROOT / "behandlingsflyt/src/main/kotlin"
LØSNING_DIR = MAIN / "no/nav/aap/behandlingsflyt/behandling/avklaringsbehov/løsning"


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8")


def rel(p: Path) -> str:
    return str(p.relative_to(ROOT))


def strip_comments(src: str) -> str:
    out, i, n = [], 0, len(src)
    while i < n:
        if src[i] == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == "\\" else 1
            out.append(src[i:j + 1])
            i = j + 1
        elif src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j == -1 else j
        elif src.startswith("/*", i):
            j = src.find("*/", i + 2)
            i = n if j == -1 else j + 2
        else:
            out.append(src[i])
            i += 1
    return "".join(out)


def match_paren(src: str, open_idx: int) -> int:
    pairs = {"(": ")", "[": "]", "{": "}"}
    stack, i = [pairs[src[open_idx]]], open_idx + 1
    while i < len(src):
        c = src[i]
        if c == '"':
            j = i + 1
            while j < len(src) and src[j] != '"':
                j += 2 if src[j] == "\\" else 1
            i = j
        elif c in pairs:
            stack.append(pairs[c])
        elif c in ")]}":
            stack.pop()
            if not stack:
                return i
        i += 1
    raise ValueError("No matching bracket")


def split_top_level(args: str) -> List[str]:
    parts, depth, cur = [], 0, []
    for c in args:
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            parts.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
    if "".join(cur).strip():
        parts.append("".join(cur).strip())
    return parts


class Def:
    def __init__(self, name: str, args: str, deprecated: bool):
        a = {}
        for part in split_top_level(args):
            m = re.match(r"(?:@param:\S+\s+)?(" + ID + r")\s*=\s*(.*)$", part, re.S)
            if m:
                a[m.group(1)] = " ".join(m.group(2).split())
        self.name = name
        self.kode = re.sub(r"\D", "", a.get("kode", ""))
        self.type = a.get("type", "").replace("BehovType.", "")
        self.steg = a.get("løsesISteg", "StegType.UDEFINERT").replace("StegType.", "")
        self.totrinn = a.get("kreverToTrinn", "false") == "true"
        self.ks = a.get("kvalitetssikres", "false") == "true"
        self.roller = re.findall(r"Rolle\.(" + ID + ")", a.get("løsesAv", ""))
        self.frist = a.get("defaultFrist", "")
        self.vilkår = a.get("vurdererVilkår", "").replace("Vilkårtype.", "").replace("null", "")
        self.deprecated = deprecated
        self.løsning: Optional[Tuple[str, Path]] = None
        self.løser: Optional[str] = None
        self.steg_klasser: List[str] = []


def load_defs() -> List[Def]:
    src = strip_comments(read(KONTRAKT / "Definisjon.kt"))
    m = re.search(r"enum\s+class\s+Definisjon\b", src)
    p = src.index("(", m.end())
    body_start = src.index("{", match_paren(src, p))
    body = src[body_start + 1:match_paren(src, body_start)]
    defs, pos, ann = [], 0, []
    while pos < len(body):
        while pos < len(body) and body[pos] in " \t\r\n,":
            pos += 1
        if pos >= len(body) or body[pos] in ";}":
            break
        if body[pos] == "@":
            am = re.match(r"@(" + ID + r")", body[pos:])
            ann.append(am.group(1))
            pos += am.end()
            if pos < len(body) and body[pos] == "(":
                pos = match_paren(body, pos) + 1
            continue
        em = re.match(ID, body[pos:])
        if not em:
            break
        name = em.group(0)
        pos += em.end()
        while body[pos] in " \t\r\n":
            pos += 1
        close = match_paren(body, pos)
        defs.append(Def(name, body[pos + 1:close], "Deprecated" in ann))
        ann, pos = [], close + 1
    return defs


def load_kode_consts() -> Dict[str, str]:
    return dict(re.findall(r"const\s+val\s+(" + ID + r")\s*:\s*String\s*=\s*\"(\d+)\"", read(KONTRAKT / "AvklaringsbehovKode.kt")))


def wire(defs: List[Def]) -> None:
    consts = load_kode_consts()
    by_kode = {d.kode: d for d in defs}
    for f in LØSNING_DIR.rglob("*.kt"):
        src = strip_comments(read(f))
        for m in re.finditer(r"@JsonTypeName\(\s*(?:value\s*=\s*)?(" + ID + r"|\"\d+\")\s*\)", src):
            token = m.group(1).strip('"')
            kode = token if token.isdigit() else consts.get(token)
            cm = re.search(r"class\s+(" + ID + ")", src[m.end():])
            d = by_kode.get(kode or "")
            if d and cm:
                d.løsning = (cm.group(1), f)
                lm = re.search(r"(" + ID + r"Løser" + r"[A-Za-z0-9_]*)\s*\(", src)
                d.løser = lm.group(1) if lm else None
    names = {d.name: d for d in defs}
    pattern = re.compile(r"Definisjon\.(" + "|".join(sorted(names, key=len, reverse=True)) + r")\b")
    for f in MAIN.rglob("*.kt"):
        src = read(f)
        if "BehandlingSteg" not in src or "Definisjon." not in src:
            continue
        cm = re.search(r"class\s+(" + ID + r")[^{]*\bBehandlingSteg\b", strip_comments(src))
        if not cm:
            continue
        for n in set(pattern.findall(strip_comments(src))):
            names[n].steg_klasser.append(cm.group(1))


def flags(d: Def) -> str:
    out = []
    if d.totrinn:
        out.append("2T")
    if d.ks:
        out.append("KS")
    if d.deprecated:
        out.append("DEPRECATED")
    return ",".join(out) or "-"


def details(d: Def) -> None:
    print(f"{d.name}  kode={d.kode}  type={d.type}" + ("  (@Deprecated)" if d.deprecated else ""))
    print(f"  løsesISteg      : {d.steg}" + ("  (UDEFINERT → løses i steget der det ble funnet: funnetISteg)" if d.steg == "UDEFINERT" else ""))
    print(f"  kreverToTrinn   : {d.totrinn}   (beslutter i FATTE_VEDTAK; løser can also force it via LøsningsResultat.kreverToTrinn)")
    print(f"  kvalitetssikres : {d.ks}   (KVALITETSSIKRING step; only faktisk førstegangsbehandling + klage)")
    print(f"  løsesAv         : {', '.join(d.roller) or '—'}")
    if d.frist:
        print(f"  defaultFrist    : {d.frist}")
    if d.vilkår:
        print(f"  vurdererVilkår  : {d.vilkår}")
    if d.løsning:
        print(f"  Løsning         : {d.løsning[0]}  {rel(d.løsning[1])}")
    else:
        print("  Løsning         : (none found – cannot be solved through /løs-behov)")
    print(f"  Løser           : {d.løser or '—'}")
    print(f"  Referenced by steps: {', '.join(sorted(set(d.steg_klasser))) or '—'}")


def table(defs: List[Def]) -> None:
    print(f"{'Definisjon':52} {'kode':5} {'type':18} {'løsesISteg':34} {'flags':6} løsesAv")
    for d in defs:
        print(f"{d.name:52} {d.kode:5} {d.type:18} {d.steg:34} {flags(d):6} {','.join(d.roller)}")
    print("\nflags: 2T = kreverToTrinn (beslutter), KS = kvalitetssikres. Details: --definisjon NAME | --kode NNNN")


def markdown(defs: List[Def]) -> str:
    out = [
        "# Avklaringsbehov definitions (generated)",
        "",
        "<!-- Generated by .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py --markdown. Do not edit by hand.",
        "     Verify freshness: python3 .agents/skills/avklaringsbehov/scripts/avklaringsbehov.py --check "
        ".agents/skills/avklaringsbehov/references/definisjoner.md -->",
        "",
        "2T = kreverToTrinn (goes to beslutter), KS = kvalitetssikres. `UDEFINERT` steg = solved in the step where it "
        "was found (ventepunkter). Steps = BehandlingSteg classes that reference the definisjon.",
        "",
        "| Definisjon | Kode | Type | løsesISteg | Flags | løsesAv | Løsning → Løser | Steps |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for d in defs:
        løsning = f"{d.løsning[0]} → {d.løser or '?'}" if d.løsning else "—"
        out.append(f"| {d.name}{' (deprecated)' if d.deprecated else ''} | {d.kode} | {d.type} | {d.steg} | {flags(d)} | "
                   f"{', '.join(d.roller)} | {løsning} | {', '.join(sorted(set(d.steg_klasser)))} |")
    out.append("")
    return "\n".join(out)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--definisjon")
    ap.add_argument("--kode")
    ap.add_argument("--steg")
    ap.add_argument("--alle", action="store_true")
    ap.add_argument("--markdown", action="store_true")
    ap.add_argument("--check", metavar="FILE")
    a = ap.parse_args()
    defs = load_defs()
    wire(defs)
    if a.check:
        cur = Path(a.check).read_text(encoding="utf-8") if Path(a.check).exists() else ""
        if cur.strip() != markdown(defs).strip():
            print(f"{a.check} is STALE. Regenerate with: python3 {rel(Path(__file__))} --markdown > {a.check}")
            sys.exit(1)
        print(f"{a.check} is up to date.")
    elif a.markdown:
        print(markdown(defs))
    elif a.definisjon or a.kode:
        hits = [d for d in defs if (a.definisjon and d.name.lower() == a.definisjon.lower()) or (a.kode and d.kode == a.kode)]
        if not hits:
            sys.exit("No such definisjon/kode")
        for d in hits:
            details(d)
    elif a.steg:
        hits = [d for d in defs if d.steg.lower() == a.steg.lower()]
        for d in hits:
            details(d)
            print()
        if not hits:
            print(f"No definisjon has løsesISteg = {a.steg}")
    else:
        table([d for d in defs if a.alle or not d.deprecated])


if __name__ == "__main__":
    main()
