#!/usr/bin/env python3
"""Inspect the behandlingsflyt flows straight from the Kotlin sources (no build needed).

Answers questions such as "in which order do the steps run?", "which class implements
StegType X?", "which steps react to Vurderingsbehov Y?" without reading 500-line files.

Usage (run from anywhere inside the repo):
  flyt.py                              list flows
  flyt.py --flyt Revurdering [--vb]    ordered steps of a flow (--vb adds vurderingsbehov per step)
  flyt.py --steg AVKLAR_SYKDOM         details for a step (StegType or class name)
  flyt.py --vurderingsbehov REVURDER_MEDLEMSKAP
                                       steps registered for a vurderingsbehov, per flow
  flyt.py --vurderingtype              Vurderingsbehov → VurderingType mapping and priority
  flyt.py --markdown                   full reference (used for references/flyter.md)
  flyt.py --check FILE                 exit 1 if FILE differs from freshly generated --markdown

Parsing is regex/paren-matching based. It understands the BehandlingFlytBuilder DSL as used in
forretningsflyt/behandlingstyper/*.kt. If a flow uses new syntax, verify against the source.
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


# ----------------------------------------------------------------------------- source helpers
def repo_root() -> Path:
    for p in Path(__file__).resolve().parents:
        if (p / "settings.gradle.kts").exists() and (p / "behandlingsflyt").is_dir():
            return p
    sys.exit("Could not locate repository root (settings.gradle.kts + behandlingsflyt/)")


ROOT = repo_root()
MAIN = ROOT / "behandlingsflyt/src/main/kotlin"
PKG = MAIN / "no/nav/aap/behandlingsflyt"
KONTRAKT = ROOT / "kontrakt/src/main/kotlin/no/nav/aap/behandlingsflyt/kontrakt"
STEGTYPE_FILE = KONTRAKT / "steg/StegType.kt"
DEFINISJON_FILE = KONTRAKT / "avklaringsbehov/Definisjon.kt"
FLOWS_DIR = PKG / "forretningsflyt/behandlingstyper"
VURDERINGSBEHOV_FILE = PKG / "sakogbehandling/flyt/Vurderingsbehov.kt"
TIDLIGERE_FILE = PKG / "behandling/vilkår/TidligereVurderingerImpl.kt"
PERIODISERING_FILE = PKG / "periodisering/FlytKontekstMedPeriodeService.kt"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))


def strip_comments(src: str) -> str:
    """Remove // and /* */ comments, keep string literals intact."""
    out = []
    i, n = 0, len(src)
    while i < n:
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j == -1 else j + 3
            out.append(src[i:j])
            i = j
        elif src[i] == '"':
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
    """Index of the bracket closing the one at open_idx (src must be comment-free)."""
    pairs = {"(": ")", "[": "]", "{": "}"}
    stack = [pairs[src[open_idx]]]
    i = open_idx + 1
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
            if c != stack[-1]:
                raise ValueError(f"Unbalanced bracket at {i}")
            stack.pop()
            if not stack:
                return i
        i += 1
    raise ValueError("No matching bracket")


def split_top_level(args: str) -> List[str]:
    parts, depth, cur, i = [], 0, [], 0
    while i < len(args):
        c = args[i]
        if c == '"':
            j = i + 1
            while j < len(args) and args[j] != '"':
                j += 2 if args[j] == "\\" else 1
            cur.append(args[i:j + 1])
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            parts.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    if "".join(cur).strip():
        parts.append("".join(cur).strip())
    return parts


def parse_enum(src: str, enum_name: str) -> List[Tuple[str, str, List[str]]]:
    """Return [(entry, args, annotations)] for `enum class enum_name`."""
    src = strip_comments(src)
    m = re.search(r"enum\s+class\s+" + enum_name + r"\b", src)
    if not m:
        raise ValueError(f"enum {enum_name} not found")
    i = src.index("{", m.end())
    if src[m.end():i].count("(") > 0:  # constructor params: find the class body brace after them
        p = src.index("(", m.end())
        i = src.index("{", match_paren(src, p))
    end = match_paren(src, i)
    body = src[i + 1:end]
    entries, pos, annotations = [], 0, []
    while pos < len(body):
        while pos < len(body) and body[pos] in " \t\r\n,":
            pos += 1
        if pos >= len(body) or body[pos] in ";}":
            break
        if body[pos] == "@":
            am = re.match(r"@(" + ID + r")", body[pos:])
            annotations.append(am.group(1))
            pos += am.end()
            if pos < len(body) and body[pos] == "(":
                pos = match_paren(body, pos) + 1
            continue
        em = re.match(ID, body[pos:])
        if not em:
            break
        name = em.group(0)
        pos += em.end()
        while pos < len(body) and body[pos] in " \t\r\n":
            pos += 1
        args = ""
        if pos < len(body) and body[pos] == "(":
            close = match_paren(body, pos)
            args = body[pos + 1:close]
            pos = close + 1
        entries.append((name, args, annotations))
        annotations = []
    return entries


def named_args(args: str) -> Dict[str, str]:
    out = {}
    for part in split_top_level(args):
        m = re.match(r"(?:@param:\S+\s+)?(" + ID + r")\s*=\s*(.*)$", part, re.S)
        if m:
            out[m.group(1)] = " ".join(m.group(2).split())
    return out


def enclosing_class(src: str, pos: int) -> Optional[str]:
    last = None
    for m in re.finditer(r"^(?:[a-z]+\s+)*class\s+(" + ID + r")", src, re.M):
        if m.start() < pos:
            last = m.group(1)
    return last


# ----------------------------------------------------------------------------- model
class StegInfo:
    def __init__(self, name: str, gruppe: str, status: str, teknisk: bool, deprecated: bool):
        self.name, self.gruppe, self.status, self.teknisk, self.deprecated = name, gruppe, status, teknisk, deprecated


class Definisjon:
    def __init__(self, name: str, a: Dict[str, str], deprecated: bool):
        self.name = name
        self.kode = re.sub(r"\D", "", a.get("kode", ""))
        self.type = a.get("type", "").replace("BehovType.", "")
        self.steg = a.get("løsesISteg", "StegType.UDEFINERT").replace("StegType.", "")
        self.totrinn = a.get("kreverToTrinn", "false") == "true"
        self.kvalitetssikres = a.get("kvalitetssikres", "false") == "true"
        self.deprecated = deprecated


class FlowStep:
    def __init__(self, klass: str, vb: str, krav: List[str], oppdater_fakta: bool):
        self.klass, self.vb_expr, self.krav, self.oppdater_fakta = klass, vb, krav, oppdater_fakta


def load_stegtyper() -> Dict[str, StegInfo]:
    out = {}
    for name, args, ann in parse_enum(read(STEGTYPE_FILE), "StegType"):
        a = named_args(args)
        out[name] = StegInfo(
            name,
            a.get("gruppe", "").replace("StegGruppe.", ""),
            a.get("status", "").replace("Status.", ""),
            a.get("tekniskSteg", "false") == "true",
            "Deprecated" in ann,
        )
    return out


def load_definisjoner() -> List[Definisjon]:
    return [Definisjon(n, named_args(a), "Deprecated" in ann)
            for n, a, ann in parse_enum(read(DEFINISJON_FILE), "Definisjon")]


def load_step_classes() -> Dict[str, Tuple[str, Path]]:
    """class name -> (StegType, file) for every `companion object : FlytSteg` in main code."""
    out = {}
    for f in (ROOT / "behandlingsflyt/src/main/kotlin").rglob("*.kt"):
        src = read(f)
        if "FlytSteg" not in src:
            continue
        src = strip_comments(src)
        for m in re.finditer(r"companion\s+object\s*:\s*([^{]*)\{", src):
            if not re.search(r"\bFlytSteg\b", m.group(1)):
                continue
            body = src[m.end() - 1:match_paren(src, m.end() - 1)]
            t = re.search(r"fun\s+type\(\)\s*(?::\s*StegType)?\s*(?:\{\s*return|=)\s*StegType\.(" + ID + ")", body)
            klass = enclosing_class(src, m.start())
            if t and klass:
                out[klass] = (t.group(1), f)
    return out


def load_krav_navn() -> Dict[str, str]:
    out = {}
    for f in MAIN.rglob("*.kt"):
        src = read(f)
        if "Informasjonskravkonstruktør" not in src:
            continue
        src = strip_comments(src)
        for m in re.finditer(r"companion\s+object\s*:\s*Informasjonskravkonstruktør\s*\{", src):
            body = src[m.end() - 1:match_paren(src, m.end() - 1)]
            n = re.search(r"InformasjonskravNavn\.(" + ID + ")", body)
            klass = enclosing_class(src, m.start())
            if n and klass:
                out[klass] = n.group(1)
    return out


def load_vurderingsbehov() -> Tuple[List[str], Dict[str, List[str]]]:
    src = read(VURDERINGSBEHOV_FILE)
    names = [n for n, _, _ in parse_enum(src, "Vurderingsbehov")]
    clean = strip_comments(src)
    funcs = {}
    for m in re.finditer(r"fun\s+(" + ID + r")\(\)\s*:\s*List<Vurderingsbehov>\s*\{", clean):
        body = clean[m.end() - 1:match_paren(clean, m.end() - 1)]
        if "listOf(" in body:
            funcs[m.group(1)] = re.findall(r"\b(" + "|".join(names) + r")\b", body)
        else:
            excluded = re.findall(r"it\s*!=\s*(" + ID + ")", body)
            funcs[m.group(1)] = [n for n in names if n not in excluded]
    return names, funcs


def load_tidligere_sjekker() -> List[str]:
    return re.findall(r"Sjekk\(StegType\.(" + ID + r")\)", strip_comments(read(TIDLIGERE_FILE)))


def load_vurderingtype() -> Tuple[List[str], Dict[str, str]]:
    """(priority order used by prioritertType, Vurderingsbehov -> VurderingType)."""
    src = strip_comments(read(PERIODISERING_FILE))
    pm = re.search(r"fun\s+prioritertType\(", src)
    pbody = src[pm.end():match_paren(src, src.index("{", pm.end()))]
    order = re.findall(r"(" + ID + r")\s+in\s+vurderingTyper\s*->", pbody)
    tm = re.search(r"fun\s+vurderingsbehovTilType\(", src)
    wstart = src.index("when", tm.end())
    tbody = src[wstart:match_paren(src, src.index("{", wstart))]
    mapping: Dict[str, str] = {}
    for branch in re.finditer(r"((?:Vurderingsbehov\.(?:" + ID + r")\s*,?\s*)+)->\s*(" + ID + ")", tbody):
        for vb in re.findall(r"Vurderingsbehov\.(" + ID + ")", branch.group(1)):
            mapping[vb] = branch.group(2)
    return order, mapping


def load_flows() -> Dict[str, dict]:
    flows = {}
    for f in sorted(FLOWS_DIR.glob("*.kt")):
        src = strip_comments(read(f))
        om = re.search(r"object\s+(" + ID + r")\s*:\s*BehandlingType", src)
        if not om:
            continue
        name = om.group(1)
        fm = re.search(r"fun\s+flyt\(\)\s*:\s*BehandlingFlyt\s*\{", src)
        body = src[fm.end() - 1:match_paren(src, fm.end() - 1)]
        alias = re.search(r"return\s+(" + ID + r")\.flyt\(\)", body)
        if "TODO(" in body:
            flows[name] = {"file": f, "todo": True, "steps": [], "alias": None}
            continue
        if alias and "BehandlingFlytBuilder" not in body:
            flows[name] = {"file": f, "todo": False, "steps": [], "alias": alias.group(1)}
            continue
        steps, oppdater, pos = [], True, 0
        while True:
            ms = body.find(".medSteg(", pos)
            mf = body.find(".sluttÅOppdatereFaktagrunnlag()", pos)
            if ms == -1 and mf == -1:
                break
            if mf != -1 and (ms == -1 or mf < ms):
                oppdater = False
                pos = mf + 1
                continue
            open_idx = ms + len(".medSteg")
            close = match_paren(body, open_idx)
            parts = split_top_level(body[open_idx + 1:close])
            pos = close
            named, positional = {}, []
            for p in parts:
                nm = re.match(r"(" + ID + r")\s*=\s*(.*)$", p, re.S)
                if nm:
                    named[nm.group(1)] = " ".join(nm.group(2).split())
                else:
                    positional.append(" ".join(p.split()))
            order = ["steg", "vurderingsbehovRelevanteForSteg", "informasjonskrav"]
            for i, v in enumerate(positional):
                named.setdefault(order[i], v)
            krav_expr = named.get("informasjonskrav", "emptyList()")
            krav = re.findall(r"(" + ID + r"Informasjonskrav)\b", krav_expr)
            steps.append(FlowStep(named["steg"], named.get("vurderingsbehovRelevanteForSteg", "?"), krav, oppdater))
        flows[name] = {"file": f, "todo": False, "steps": steps, "alias": None}
    return flows


class Model:
    def __init__(self):
        self.stegtyper = load_stegtyper()
        self.definisjoner = load_definisjoner()
        self.classes = load_step_classes()
        self.krav_navn = load_krav_navn()
        self.vb_names, self.vb_funcs = load_vurderingsbehov()
        self.tidligere = load_tidligere_sjekker()
        self.vt_order, self.vt_map = load_vurderingtype()
        self.flows = load_flows()

    def steg_of(self, klass: str) -> str:
        return self.classes.get(klass, ("?", None))[0]

    def vb_list(self, expr: str) -> Optional[List[str]]:
        expr = expr.strip()
        if expr in ("emptyList()", "listOf()"):
            return []
        fm = re.match(r"Vurderingsbehov\.(" + ID + r")\(\)$", expr)
        if fm:
            return self.vb_funcs.get(fm.group(1))
        if expr.startswith("listOf("):
            return re.findall(r"Vurderingsbehov\.(" + ID + r")", expr)
        return None

    def vb_short(self, expr: str) -> str:
        fm = re.match(r"Vurderingsbehov\.(" + ID + r")\(\)$", expr.strip())
        if fm:
            return f"Vurderingsbehov.{fm.group(1)}()"
        lst = self.vb_list(expr)
        if lst is None:
            return expr
        return ", ".join(lst) if lst else "—"

    def defs_for(self, steg: str) -> List[Definisjon]:
        return [d for d in self.definisjoner if d.steg == steg]

    def concrete_flows(self) -> List[str]:
        return [n for n, f in self.flows.items() if not f["alias"] and not f["todo"]]

    def aliases_of(self, flow: str) -> List[str]:
        return [n for n, f in self.flows.items() if f["alias"] == flow]


def fmt_def(d: Definisjon) -> str:
    flags = [d.kode, d.type]
    if d.totrinn:
        flags.append("totrinn")
    if d.kvalitetssikres:
        flags.append("kvalitetssikres")
    if d.deprecated:
        flags.append("DEPRECATED")
    return f"{d.name} ({', '.join(flags)})"


# ----------------------------------------------------------------------------- commands
def cmd_list(m: Model) -> None:
    print("Flows (forretningsflyt/behandlingstyper):")
    for name, f in m.flows.items():
        if f["todo"]:
            print(f"  {name:24} not implemented (TODO)")
        elif f["alias"]:
            print(f"  {name:24} = {f['alias']}.flyt()  (same steps)")
        else:
            print(f"  {name:24} {len(f['steps'])} steps")
    print("\nTry: --flyt Revurdering | --steg AVKLAR_SYKDOM | --vurderingsbehov MOTTATT_SØKNAD | --markdown")


def resolve_flow(m: Model, name: str) -> str:
    for n in m.flows:
        if n.lower() == name.lower():
            return m.flows[n]["alias"] or n
    sys.exit(f"Unknown flow {name}. Known: {', '.join(m.flows)}")


def cmd_flyt(m: Model, name: str, with_vb: bool) -> None:
    flow = resolve_flow(m, name)
    aliases = m.aliases_of(flow)
    print(f"Flow {flow}" + (f" (also used by {', '.join(aliases)})" if aliases else "") + f"  [{rel(m.flows[flow]['file'])}]")
    print(f"{'#':>3}  {'StegType':34} {'Class':34} {'Status':11} {'Gruppe':22} Informasjonskrav | Avklaringsbehov løst her")
    marker_printed = False
    for i, s in enumerate(m.flows[flow]["steps"], 1):
        if not s.oppdater_fakta and not marker_printed:
            print("     ---- sluttÅOppdatereFaktagrunnlag(): below, informasjonskrav are not refreshed in forbered and "
                  "kontekst gets ALL vurderingsbehov ----")
            marker_printed = True
        st = m.steg_of(s.klass)
        info = m.stegtyper.get(st)
        krav = ", ".join(k.replace("Informasjonskrav", "") for k in s.krav) or "—"
        defs = ", ".join(d.name for d in m.defs_for(st) if not d.deprecated) or "—"
        print(f"{i:>3}  {st:34} {s.klass:34} {info.status if info else '?':11} {info.gruppe if info else '?':22} {krav} | {defs}")
        if with_vb:
            print(f"{'':6}vurderingsbehov: {m.vb_short(s.vb_expr)}")


def find_steg(m: Model, query: str) -> Tuple[str, Optional[str]]:
    q = query.strip()
    for klass, (st, _) in m.classes.items():
        if klass.lower() == q.lower():
            return st, klass
    for st in m.stegtyper:
        if st.lower() == q.lower():
            klass = next((k for k, (s, _) in m.classes.items() if s == st), None)
            return st, klass
    sys.exit(f"No StegType or step class named {query}")


def cmd_steg(m: Model, query: str) -> None:
    st, klass = find_steg(m, query)
    info = m.stegtyper[st]
    print(f"StegType : {st}   gruppe={info.gruppe}  behandlingsstatus={info.status}"
          + ("  tekniskSteg" if info.teknisk else "") + ("  DEPRECATED" if info.deprecated else ""))
    if klass:
        print(f"Class    : {klass}   {rel(m.classes[klass][1])}")
    else:
        print("Class    : (none in main code – StegType unused or deprecated)")
    defs = m.defs_for(st)
    print("Avklaringsbehov with løsesISteg = this step:")
    for d in defs:
        print(f"  - {fmt_def(d)}")
    if not defs:
        print("  (none)")
    print(f"TidligereVurderinger has an avslag/grunnlag check for this step: {'yes' if st in m.tidligere else 'no'}")
    found = False
    for flow in m.concrete_flows():
        steps = m.flows[flow]["steps"]
        for i, s in enumerate(steps):
            if m.steg_of(s.klass) != st:
                continue
            found = True
            aliases = m.aliases_of(flow)
            prev_s = m.steg_of(steps[i - 1].klass) if i > 0 else "—"
            next_s = m.steg_of(steps[i + 1].klass) if i + 1 < len(steps) else "— (end → behandling AVSLUTTET)"
            print(f"\nIn flow {flow} (#{i + 1} of {len(steps)})" + (f", also {', '.join(aliases)}" if aliases else ""))
            print(f"  previous/next     : {prev_s} / {next_s}")
            krav = [f"{k} ({m.krav_navn.get(k, '?')})" for k in s.krav]
            print(f"  informasjonskrav  : {', '.join(krav) or '—'}")
            print(f"  vurderingsbehov   : {m.vb_short(s.vb_expr)}")
            print(f"  oppdaterFaktagrunnlag: {s.oppdater_fakta}"
                  + ("" if s.oppdater_fakta else "  (after sluttÅOppdatereFaktagrunnlag: kontekst gets ALL vurderingsbehov)"))
    if not found:
        print("\nNot part of any flow.")


def cmd_vb(m: Model, vb: str) -> None:
    match = [n for n in m.vb_names if n.lower() == vb.lower()]
    if not match:
        sys.exit(f"Unknown Vurderingsbehov {vb}")
    vb = match[0]
    print(f"{vb} → VurderingType {m.vt_map.get(vb, '?')}  (behandling-level; see --vurderingtype)")
    print(f"Steps registered for {vb} (kontekst.vurderingsbehovRelevanteForSteg + rewind target when added later):")
    for flow in m.concrete_flows():
        hits = []
        for i, s in enumerate(m.flows[flow]["steps"], 1):
            lst = m.vb_list(s.vb_expr)
            if lst is None:
                hits.append(f"#{i} {m.steg_of(s.klass)} (unparsed: {s.vb_expr})")
            elif vb in lst:
                hits.append(f"#{i} {m.steg_of(s.klass)}")
        first = next((h for h in hits if m.stegtyper.get(h.split()[1], StegInfo('', '', '', False, False)).status != "OPPRETTET"), None)
        print(f"\n{flow}: {len(hits)} steps" + (f"; earliest non-OPPRETTET (rewind target): {first}" if first else ""))
        for h in hits:
            print(f"  {h}")


def markdown(m: Model) -> str:
    out = [
        "# Flows (generated)",
        "",
        "<!-- Generated by .agents/skills/flyt-orkestrering/scripts/flyt.py --markdown. Do not edit by hand.",
        "     Verify freshness: python3 .agents/skills/flyt-orkestrering/scripts/flyt.py --check "
        ".agents/skills/flyt-orkestrering/references/flyter.md -->",
        "",
        "Status = behandling status while the step is active. Gruppe = StegGruppe (UI grouping).",
        "Krav = informasjonskrav registered on the step. Vurderingsbehov = `vurderingsbehovRelevanteForSteg` "
        "(filters kontekst + drives rewind; it never skips the step).",
        "",
    ]
    for name, f in m.flows.items():
        if f["todo"]:
            out += [f"## {name}", "", "Not implemented (`TODO()`).", ""]
        elif f["alias"]:
            out += [f"## {name}", "", f"`{name}.flyt()` returns `{f['alias']}.flyt()` — identical steps.", ""]
    for flow in m.concrete_flows():
        aliases = m.aliases_of(flow)
        out += [f"## {flow}" + (f" (also {', '.join(aliases)})" if aliases else ""), "",
                f"Source: `{rel(m.flows[flow]['file'])}`", "",
                "| # | StegType | Class | Status | Gruppe | Krav | Avklaringsbehov (løses her) | Vurderingsbehov |",
                "|---|---|---|---|---|---|---|---|"]
        marker = False
        for i, s in enumerate(m.flows[flow]["steps"], 1):
            if not s.oppdater_fakta and not marker:
                out.append("| | **`sluttÅOppdatereFaktagrunnlag()`** | | | | | | *below: no informasjonskrav refresh in forbered; kontekst gets ALL vurderingsbehov* |")
                marker = True
            st = m.steg_of(s.klass)
            info = m.stegtyper.get(st)
            krav = ", ".join(k.replace("Informasjonskrav", "") for k in s.krav) or ""
            defs = ", ".join(f"{d.name} ({d.kode})" for d in m.defs_for(st) if not d.deprecated)
            out.append(f"| {i} | {st} | {s.klass} | {info.status if info else '?'} | {info.gruppe if info else '?'} | "
                       f"{krav} | {defs} | {m.vb_short(s.vb_expr)} |")
        out.append("")
    main = "Revurdering" if "Revurdering" in m.flows else m.concrete_flows()[0]
    out += ["## VurderingType", "",
            "`FlytKontekstMedPeriodeService`: each vurderingsbehov maps to a VurderingType; the behandling's "
            "`kontekst.vurderingType` is the highest-priority type among ALL its vurderingsbehov (same value for every "
            "step). Exception: TypeBehandling.Førstegangsbehandling always gets FØRSTEGANGSBEHANDLING unless "
            "MIGERING_FRA_ARENA is among the types.", "",
            "Priority (highest first): " + " > ".join(m.vt_order) + " > IKKE_RELEVANT", ""]
    by_type: Dict[str, List[str]] = {}
    for vb in m.vb_names:
        by_type.setdefault(m.vt_map.get(vb, "?"), []).append(vb)
    out += ["| VurderingType | Vurderingsbehov |", "|---|---|"]
    for t in m.vt_order + ["IKKE_RELEVANT", "?"]:
        if t in by_type:
            out.append(f"| {t} | {', '.join(by_type[t])} |")
    out.append("")
    out += [f"## Vurderingsbehov → earliest reacting step in {main}", "",
            "Earliest step (status ≠ OPPRETTET) whose list contains the vurderingsbehov. When the vurderingsbehov is "
            "added after that step has run, `forberedBehandling` rewinds there.", "",
            "| Vurderingsbehov | Earliest step | # steps reacting |", "|---|---|---|"]
    for vb in m.vb_names:
        hits = [(i, m.steg_of(s.klass)) for i, s in enumerate(m.flows[main]["steps"], 1)
                if vb in (m.vb_list(s.vb_expr) or [])]
        first = next((f"#{i} {st}" for i, st in hits if m.stegtyper[st].status != "OPPRETTET"), "—")
        out.append(f"| {vb} | {first} | {len(hits)} |")
    out.append("")
    return "\n".join(out)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--flyt")
    ap.add_argument("--vb", action="store_true", help="with --flyt: show vurderingsbehov per step")
    ap.add_argument("--steg")
    ap.add_argument("--vurderingsbehov")
    ap.add_argument("--vurderingtype", action="store_true", help="Vurderingsbehov → VurderingType mapping + priority")
    ap.add_argument("--markdown", action="store_true")
    ap.add_argument("--check", metavar="FILE")
    a = ap.parse_args()
    m = Model()
    if a.check:
        current = Path(a.check).read_text(encoding="utf-8") if Path(a.check).exists() else ""
        if current.strip() != markdown(m).strip():
            print(f"{a.check} is STALE. Regenerate with: python3 {rel(Path(__file__))} --markdown > {a.check}")
            sys.exit(1)
        print(f"{a.check} is up to date.")
    elif a.markdown:
        print(markdown(m))
    elif a.flyt:
        cmd_flyt(m, a.flyt, a.vb)
    elif a.steg:
        cmd_steg(m, a.steg)
    elif a.vurderingsbehov:
        cmd_vb(m, a.vurderingsbehov)
    elif a.vurderingtype:
        print("Priority (highest first): " + " > ".join(m.vt_order) + " > IKKE_RELEVANT")
        print("TypeBehandling.Førstegangsbehandling → always FØRSTEGANGSBEHANDLING unless MIGERING_FRA_ARENA is present.\n")
        for vb in m.vb_names:
            print(f"  {vb:46} → {m.vt_map.get(vb, '?')}")
    else:
        cmd_list(m)


if __name__ == "__main__":
    main()
