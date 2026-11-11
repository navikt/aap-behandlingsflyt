#!/usr/bin/env python3
"""Find, batch, run and summarise tests in aap-behandlingsflyt. Always module-scoped ./gradlew commands.

Usage:
  kjor_test.py AlderFlytTest                      print the command for one class
  kjor_test.py AlderFlytTest -m "stopper ikke"    only test methods whose name contains the text
  kjor_test.py KlasseA KlasseB KlasseC            several classes -> commands per module; flyt-tests in small batches
  kjor_test.py AlderFlytTest --run                run it (exactly one command); full log to a file, summary printed
  kjor_test.py KlasseA KlasseB --run 2            run command no. 2 of the printed list (one command per tool call)
  kjor_test.py --liste flyt|integrasjon|enhet|alle [--modul behandlingsflyt]
  kjor_test.py --batch flyt|integrasjon|enhet [--modul m] [--run N]
                                                  everything of one kind, split into commands you run one at a time
  kjor_test.py --endret [REF]                     tests related to changed files (git; heuristic) + commands
  kjor_test.py --resultat [modul ...]             summarise JUnit XML results: failures, durations, swallowed job errors
  kjor_test.py --dsl [tekst]                      list flyt-test DSL helpers in AbstraktFlytOrkestratorTest
  kjor_test.py --sjekk-miljo                      check JDK, Docker and the Gradle wrapper

Kinds (heuristic, from the test source):
  flyt        extends AbstraktFlytOrkestratorTest / AbstraktFlytOrkestratorSnapshotTest (Postgres + motor + all fakes)
  integrasjon TestDataSource/Testcontainers, @Fakes (HTTP fake servers), Ktor testApplication or an embedded server
  enhet       everything else (no I/O, fakes in memory)

Never run the whole suite (./gradlew test/check/build): it is slow and times out in agent sandboxes. CI runs it.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from collections import OrderedDict, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def repo_root() -> Path:
    for p in Path(__file__).resolve().parents:
        if (p / "settings.gradle.kts").exists() and (p / "behandlingsflyt").is_dir():
            return p
    sys.exit("Could not locate repository root")


ROOT = repo_root()
MODULES = ["behandlingsflyt", "api", "app", "repository", "kontrakt", "lib-test"]
ARTER = ("flyt", "integrasjon", "enhet")

# Max (classes, test invocations) per Gradle command. Flyt-tests start Postgres + motor + fakes and drive whole
# behandlinger; several of them in one run is what makes agent tool calls time out.
GRENSER: Dict[str, Tuple[int, int]] = {
    "flyt": (3, 20),
    "integrasjon": (40, 300),
    "enhet": (100_000, 100_000),
}

PACKAGE = re.compile(r"^package\s+([\w.]+)", re.M)
TEST_ANNOT = re.compile(r"^[ \t]*@(?:Test|ParameterizedTest|TableTest|RepeatedTest|TestFactory)\b", re.M)
CLASS_DECL = re.compile(
    r"^(?P<mods>(?:(?:internal|open|abstract|private|public|data|sealed|enum|annotation|inner|value)\s+)*)"
    r"class\s+(?P<navn>\w+)",
    re.M,
)
DOCKER_MARKERS = re.compile(
    r"\bTestDataSource\b|postgreSQLContainer|PostgreSQLContainer|KafkaContainer|SharedKafkaTestContainer"
)
INTEGRASJON_MARKERS = re.compile(
    r"@Fakes\b|testApplication\s*\{|embeddedServer\(|\bTestDataSource\b|postgreSQLContainer|PostgreSQLContainer"
    r"|KafkaContainer|SharedKafkaTestContainer|initDatasource\("
)
FLYT_BASER = {"AbstraktFlytOrkestratorTest", "AbstraktFlytOrkestratorSnapshotTest"}
JOBBFEIL = ("Feil under prosessering av jobb", "Feil under plukking av jobber")
ANSI = re.compile(r"\x1b\[[0-9;]*m")


@dataclass
class TestKlasse:
    modul: str
    fqcn: str
    navn: str
    fil: Path
    art: str
    docker: bool
    tester: int
    faktor: int  # 2 for @ParameterizedClass over unleashTestDataSource (every test runs twice)

    @property
    def kost(self) -> int:
        return max(1, self.tester) * self.faktor

    @property
    def pakke(self) -> str:
        return self.fqcn.rsplit(".", 1)[0] if "." in self.fqcn else ""


# ----------------------------------------------------------------------------------------------- indexing

def _header(tekst: str, start: int) -> str:
    slutt = tekst.find("{", start)
    return tekst[start: slutt if slutt != -1 else start + 400]


def _annotasjoner_over(tekst: str, start: int) -> str:
    linjer = tekst[:start].splitlines()
    blokk = []
    for linje in reversed(linjer):
        s = linje.strip()
        if s.startswith("@") or (blokk and s.startswith(")")):
            blokk.append(s)
        elif s == "" and not blokk:
            continue
        else:
            break
    return "\n".join(blokk)


def _les_filer() -> List[Tuple[str, Path, str]]:
    filer = []
    for modul in MODULES:
        base = ROOT / modul / "src" / "test" / "kotlin"
        if base.is_dir():
            for fil in sorted(base.rglob("*.kt")):
                filer.append((modul, fil, fil.read_text(encoding="utf-8", errors="replace")))
    return filer


def indekser() -> List[TestKlasse]:
    filer = _les_filer()
    baser = set(FLYT_BASER)
    endret = True
    while endret:  # abstract/open helpers that extend a flyt base are flyt bases too
        endret = False
        for _, _, tekst in filer:
            for m in CLASS_DECL.finditer(tekst):
                if m.group("navn") not in baser and any(re.search(rf"\b{b}\s*\(", _header(tekst, m.start()))
                                                        for b in baser):
                    if "abstract" in m.group("mods") or "open" in m.group("mods"):
                        baser.add(m.group("navn"))
                        endret = True
    flyt_re = re.compile(r"\b(?:%s)\s*\(" % "|".join(sorted(baser)))

    klasser: List[TestKlasse] = []
    for modul, fil, tekst in filer:
        testposisjoner = [m.start() for m in TEST_ANNOT.finditer(tekst)]
        if not testposisjoner:
            continue
        pkg_m = PACKAGE.search(tekst)
        pkg = pkg_m.group(1) if pkg_m else ""
        decls = list(CLASS_DECL.finditer(tekst))
        antall: Dict[int, int] = defaultdict(int)
        for pos in testposisjoner:
            eier = None
            for i, d in enumerate(decls):
                if d.start() < pos:
                    eier = i
            if eier is not None:
                antall[eier] += 1
        integrasjon = bool(INTEGRASJON_MARKERS.search(tekst))
        docker = bool(DOCKER_MARKERS.search(tekst))
        for i, d in enumerate(decls):
            mods = d.group("mods")
            if antall.get(i, 0) == 0 or any(x in mods for x in ("abstract", "data", "sealed", "enum", "annotation")):
                continue
            navn = d.group("navn")
            if navn in baser:
                continue
            er_flyt = bool(flyt_re.search(_header(tekst, d.start())))
            art = "flyt" if er_flyt else ("integrasjon" if integrasjon else "enhet")
            faktor = 2 if "@ParameterizedClass" in _annotasjoner_over(tekst, d.start()) else 1
            klasser.append(TestKlasse(modul, f"{pkg}.{navn}" if pkg else navn, navn, fil, art,
                                      docker or er_flyt, antall[i], faktor))
    return klasser


# ----------------------------------------------------------------------------------------------- commands

def _q(s: str) -> str:
    return "'" + s.replace("'", "'\"'\"'") + "'"


def gradle_kommando(modul: str, mønstre: List[str], rerun: bool) -> List[str]:
    cmd = ["./gradlew", f":{modul}:test"]
    for m in mønstre:
        cmd += ["--tests", m]
    if rerun:
        cmd.append("--rerun")
    return cmd


def vis(cmd: List[str]) -> str:
    ut = []
    for i, c in enumerate(cmd):
        ut.append(_q(c) if i > 0 and cmd[i - 1] == "--tests" else c)
    return " ".join(ut)


def _batcher(elementer: List[Tuple[str, int, int]], maks_klasser: int, maks_kost: int) -> List[List[str]]:
    """elementer: (mønster, antall klasser, kost). First-fit decreasing: few batches, heaviest first."""
    batcher: List[Tuple[List[str], int, int]] = []
    for mønster, kl, kost in sorted(elementer, key=lambda e: (-e[2], e[0])):
        for i, (mønstre, b_kl, b_kost) in enumerate(batcher):
            if b_kl + kl <= maks_klasser and b_kost + kost <= maks_kost:
                batcher[i] = (mønstre + [mønster], b_kl + kl, b_kost + kost)
                break
        else:
            batcher.append(([mønster], kl, kost))
    return [mønstre for mønstre, _, _ in batcher]


def komprimer(valgte: List[TestKlasse], alle_i_modul: List[TestKlasse]) -> List[Tuple[str, int, int]]:
    """Use 'pakke.*' where every test class under the package is selected (shorter commands)."""
    valgt = {k.fqcn for k in valgte}
    under: Dict[str, List[TestKlasse]] = defaultdict(list)
    for k in alle_i_modul:
        deler = k.pakke.split(".")
        for i in range(4, len(deler) + 1):  # never shorter than no.nav.aap.<x>
            under[".".join(deler[:i])].append(k)
    dekket: List[str] = []
    ut: List[Tuple[str, int, int]] = []
    for pkg in sorted(under, key=lambda p: (p.count("."), p)):
        if any(pkg == d or pkg.startswith(d + ".") for d in dekket):
            continue
        ks = under[pkg]
        if all(k.fqcn in valgt for k in ks):
            dekket.append(pkg)
            ut.append((pkg + ".*", len(ks), sum(k.kost for k in ks)))
    for k in valgte:
        if not any(k.pakke == d or k.pakke.startswith(d + ".") for d in dekket):
            ut.append((k.fqcn, 1, k.kost))
    return ut


def lag_kommandoer(klasser: List[TestKlasse], indeks: List[TestKlasse], metode: Optional[str], rerun: bool,
                   komprimer_pakker: bool) -> List[Tuple[str, str, List[str]]]:
    """Returns (modul, art, cmd) – one Gradle invocation each, grouped per module and kind."""
    per_modul: "OrderedDict[str, List[TestKlasse]]" = OrderedDict()
    for k in klasser:
        per_modul.setdefault(k.modul, []).append(k)
    ut = []
    for modul, ks in per_modul.items():
        for art in ("enhet", "integrasjon", "flyt"):
            gruppe = [k for k in ks if k.art == art]
            if not gruppe:
                continue
            if komprimer_pakker and art != "flyt" and not metode:
                elementer = komprimer(gruppe, [k for k in indeks if k.modul == modul])
            else:
                elementer = [(k.fqcn + (f".*{metode}*" if metode else ""), 1, k.kost) for k in gruppe]
            maks_kl, maks_kost = GRENSER[art]
            if modul == "app" and art == "integrasjon":  # whole server + Kafka per class: as heavy as flyt-tests
                maks_kl, maks_kost = GRENSER["flyt"]
            for batch in _batcher(elementer, maks_kl, maks_kost):
                ut.append((modul, art, gradle_kommando(modul, batch, rerun)))
    return ut


def skriv_kommandoer(kommandoer: List[Tuple[str, str, List[str]]]) -> None:
    if len(kommandoer) > 1:
        print(f"# {len(kommandoer)} commands: run them ONE PER TOOL CALL (or `--run N`), check the result before the next.")
    for nr, (modul, art, cmd) in enumerate(kommandoer, 1):
        prefix = f"[{nr}] " if len(kommandoer) > 1 else ""
        print(f"{prefix}{vis(cmd)}    # {art}")


# ----------------------------------------------------------------------------------------------- results

def _alder(sekunder: float) -> str:
    if sekunder < 120:
        return f"{int(sekunder)} s ago"
    if sekunder < 7200:
        return f"{int(sekunder // 60)} min ago"
    return f"{sekunder / 3600:.1f} h ago"


def _første_relevante_ramme(tekst: str) -> str:
    for linje in tekst.splitlines()[1:]:
        s = linje.strip()
        if s.startswith("at no.nav.aap") and "Test" in s:
            return s
    for linje in tekst.splitlines()[1:]:
        s = linje.strip()
        if s.startswith("at no.nav.aap"):
            return s
    return ""


def resultat(moduler: List[str], bare_klasser: Optional[set] = None, siden: Optional[float] = None) -> int:
    feilet_totalt = 0
    for modul in moduler or MODULES:
        mappe = ROOT / modul / "build" / "test-results" / "test"
        filer = sorted(mappe.glob("TEST-*.xml")) if mappe.is_dir() else []
        if bare_klasser is not None:
            filer = [f for f in filer if f.name[5:-4] in bare_klasser]
        if siden is not None:
            filer = [f for f in filer if f.stat().st_mtime >= siden]
        if not filer:
            if moduler:
                print(f"{modul}: no JUnit XML results (run tests first)")
            continue
        nå = time.time()
        totalt = feil = hoppet = 0
        varighet: List[Tuple[float, str]] = []
        feilmeldinger: List[str] = []
        jobbfeil: Dict[str, List[str]] = defaultdict(list)
        nyeste = max(f.stat().st_mtime for f in filer)
        eldste = min(f.stat().st_mtime for f in filer)
        for f in filer:
            try:
                suite = ET.parse(f).getroot()
            except ET.ParseError:
                print(f"  (could not parse {f.name})")
                continue
            klasse = suite.get("name", f.name)
            kort = klasse.rsplit(".", 1)[-1]
            totalt += int(suite.get("tests", 0))
            hoppet += int(suite.get("skipped", 0))
            varighet.append((float(suite.get("time", 0) or 0), kort))
            for case in suite.iter("testcase"):
                for tag in ("failure", "error"):
                    el = case.find(tag)
                    if el is not None:
                        feil += 1
                        melding = (el.get("message") or (el.text or "").strip().splitlines()[0:1] or [""])
                        melding = melding if isinstance(melding, str) else melding[0]
                        melding = " ".join(melding.split())[:300]
                        ramme = _første_relevante_ramme(el.text or "")
                        feilmeldinger.append(f"FAILED {kort} > {case.get('name')}  ({case.get('time')} s)\n"
                                             f"    {el.get('type', '')}: {melding}" + (f"\n    {ramme}" if ramme else ""))
            for strøm in ("system-out", "system-err"):
                el = suite.find(strøm)
                if el is None or not el.text:
                    continue
                linjer = ANSI.sub("", el.text).splitlines()
                for i, linje in enumerate(linjer):
                    if any(m in linje for m in JOBBFEIL):
                        typ = re.search(r"jobb \[([^\]]+)\]", linje)
                        unntak = linjer[i + 1].strip() if i + 1 < len(linjer) else ""
                        jobbfeil[kort].append(f"{typ.group(1) if typ else '?'} -> {unntak[:200]}")
        feilet_totalt += feil
        spenn = f", files written over {int((nyeste - eldste) // 60)} min" if nyeste - eldste > 600 else ""
        print(f"{modul}: {len(filer)} classes, {totalt} tests, {feil} failed, {hoppet} skipped "
              f"(newest result {_alder(nå - nyeste)}{spenn})")
        for linje in feilmeldinger[:30]:
            print(linje)
        if len(feilmeldinger) > 30:
            print(f"... and {len(feilmeldinger) - 30} more failures")
        if jobbfeil:
            print("Swallowed motor job errors (ManuellMotorImpl logs them and marks the job FEILET; the test continues):")
            for kort, liste in jobbfeil.items():
                unike = list(OrderedDict.fromkeys(liste))
                print(f"  {kort}: {len(liste)}x " + " | ".join(unike[:3]))
        tregeste = sorted(varighet, reverse=True)[:5]
        print("Slowest classes: " + ", ".join(f"{n} {t:.0f} s" for t, n in tregeste))
    return feilet_totalt


# ----------------------------------------------------------------------------------------------- running

def kjør(cmd: List[str], klasser: List[TestKlasse]) -> int:
    logg = Path(tempfile.gettempdir()) / f"kjor_test-{time.strftime('%Y%m%d-%H%M%S')}.log"
    print(f"$ {vis(cmd)}\n# full Gradle output: {logg}", flush=True)
    start = time.time()
    with open(logg, "w", encoding="utf-8") as ut:
        rc = subprocess.run(cmd, cwd=ROOT, stdout=ut, stderr=subprocess.STDOUT).returncode
    sek = time.time() - start
    tekst = logg.read_text(encoding="utf-8", errors="replace").splitlines()
    viktige = [l for l in tekst if re.search(
        r"FAILED|BUILD (SUCCESSFUL|FAILED)|^e: |error:|What went wrong|No tests found|Could not find a valid Docker"
        r"|> Task :[\w-]+:test (UP-TO-DATE|FROM-CACHE)|actionable task", l)]
    print(f"# exit code {rc} after {sek:.0f} s; console: {sum('PASSED' in l for l in tekst)} passed, "
          f"{sum(l.rstrip().endswith('FAILED') for l in tekst)} failed, {sum('SKIPPED' in l and ' > ' in l for l in tekst)} skipped")
    for l in viktige[-25:]:
        print(l)
    if any("No tests found" in l for l in tekst):
        print("# 'No tests found': the filter matched nothing (wrong module/class/method name?)")
    if rc == 0 and not any("PASSED" in l or "FAILED" in l for l in tekst):
        print("# no test lines in the output: the test task was probably UP-TO-DATE/FROM-CACHE; add --rerun to force")
    moduler = sorted({k.modul for k in klasser}) or [cmd[1].split(":")[1]]
    resultat(moduler, siden=start - 5)
    return rc


# ----------------------------------------------------------------------------------------------- --endret

def _git(*args: str) -> List[str]:
    res = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)
    return [l for l in res.stdout.splitlines() if l.strip()] if res.returncode == 0 else []


def endret(ref: Optional[str], indeks: List[TestKlasse]) -> List[TestKlasse]:
    filer = set(_git("diff", "--name-only", ref) if ref else [])
    filer |= set(_git("diff", "--name-only")) | set(_git("diff", "--name-only", "--cached"))
    filer |= set(_git("ls-files", "--others", "--exclude-standard"))
    kt = sorted(f for f in filer if f.endswith(".kt") and (ROOT / f).exists())
    if not kt:
        print("No changed .kt files found." + ("" if ref else " (Compare with a branch: --endret origin/main)"))
        return []
    per_fil = {k.fil.relative_to(ROOT).as_posix(): k for k in indeks}
    direkte: "OrderedDict[str, TestKlasse]" = OrderedDict()
    navn: List[str] = []
    for f in kt:
        if "/src/test/" in f:
            for k in indeks:
                if k.fil.relative_to(ROOT).as_posix() == f:
                    direkte[k.fqcn] = k
            continue
        tekst = (ROOT / f).read_text(encoding="utf-8", errors="replace")
        navn.append(Path(f).stem)
        navn += re.findall(r"^(?:[a-z]+\s+)*(?:class|object|interface)\s+(\w+)", tekst, re.M)
    navn = list(OrderedDict.fromkeys(n for n in navn if len(n) > 3))
    for n in navn:
        for k in indeks:
            if k.navn == f"{n}Test" or (k.navn.startswith(n) and k.navn.endswith("Test")):
                direkte.setdefault(k.fqcn, k)
    treff: Dict[str, int] = defaultdict(int)
    if navn:
        mønster = re.compile(r"\b(?:%s)\b" % "|".join(re.escape(n) for n in navn))
        for k in indeks:
            if k.fqcn in direkte:
                continue
            antall = len(mønster.findall(k.fil.read_text(encoding="utf-8", errors="replace")))
            if antall:
                treff[k.fqcn] = antall
    print(f"Changed .kt files: {len(kt)}; names looked up: {', '.join(navn[:12])}{' …' if len(navn) > 12 else ''}")
    print(f"\nDirect (changed test files, or <Name>Test): {len(direkte)}")
    for k in list(direkte.values())[:25]:
        print(f"  {k.art:<11} {k.modul:<15} {k.fqcn}")
    refererende = sorted(treff, key=lambda q: -treff[q])
    efter_art: Dict[str, List[str]] = defaultdict(list)
    alle = {k.fqcn: k for k in indeks}
    for q in refererende:
        efter_art[alle[q].art].append(q)
    for art in ARTER:
        liste = efter_art.get(art, [])
        if liste:
            print(f"\nReferencing the changed names ({art}): {len(liste)}  — pick the relevant ones")
            for q in liste[:15]:
                print(f"  {treff[q]:>3} refs  {alle[q].modul:<15} {q}")
            if len(liste) > 15:
                print(f"  ... and {len(liste) - 15} more")
    return list(direkte.values())


# ----------------------------------------------------------------------------------------------- --dsl

def dsl(filter_tekst: Optional[str]) -> None:
    fil = ROOT / "behandlingsflyt/src/test/kotlin/no/nav/aap/behandlingsflyt/flyt/AbstraktFlytOrkestratorTest.kt"
    tekst = fil.read_text(encoding="utf-8")
    for m in re.finditer(r"^[ \t]*((?:protected |public |internal |open )*)fun\s+([\w.<>]+?\.)?(\w+)\s*\(", tekst, re.M):
        if m.group(3) in ("setup", "tearDown", "unleashTestDataSource", "beforeEachClearDatabase"):
            continue
        dybde, i = 1, m.end()
        while i < len(tekst) and dybde:
            dybde += {"(": 1, ")": -1}.get(tekst[i], 0)
            i += 1
        params = " ".join(tekst[m.end():i - 1].split())
        rest = tekst[i:tekst.find("\n", i)].strip()
        retur = re.match(r":\s*([\w<>?, .]+)", rest)
        sig = f"{m.group(2) or ''}{m.group(3)}({params})" + (f": {retur.group(1).strip()}" if retur else "")
        if filter_tekst and filter_tekst.lower() not in sig.lower():
            continue
        linje = tekst.count("\n", 0, m.start()) + 1
        print(f"L{linje:<5}{sig[:170]}")
    print(f"# {fil.relative_to(ROOT)} — read the helper before using it; add new helpers there.")


# ----------------------------------------------------------------------------------------------- environment

def sjekk_miljo() -> bool:
    ok = True
    if not (ROOT / "gradlew").exists():
        print("✗ ./gradlew missing")
        ok = False
    else:
        print("✓ ./gradlew present (always use it; never a globally installed gradle)")
    java = shutil.which("java")
    res = subprocess.run([java, "-version"], capture_output=True, text=True) if java else None
    linje = next((l for l in (res.stderr.splitlines() if res else []) if "version" in l), None)
    if not res or res.returncode != 0 or not linje:
        print("✗ no working java (macOS has a /usr/bin/java stub). Gradle needs a JDK to start; the build uses a "
              "JDK 25 toolchain (auto-detected or downloaded).")
        ok = False
    else:
        print(f"✓ java: {linje.strip()}")
    sock = os.environ.get("DOCKER_HOST", "")
    kandidater = [Path("/var/run/docker.sock"), Path.home() / ".colima/docker.sock",
                  Path.home() / ".colima/default/docker.sock"]
    funnet = sock or next((str(c) for c in kandidater if c.exists()), "")
    docker_ok = False
    if shutil.which("docker"):
        try:
            r = subprocess.run(["docker", "info", "--format", "{{.ServerVersion}}"], capture_output=True, text=True,
                               timeout=10)
            docker_ok = r.returncode == 0 and r.stdout.strip() != ""
            if docker_ok:
                print(f"✓ docker daemon reachable (server {r.stdout.strip()})")
        except subprocess.TimeoutExpired:
            pass
    if not docker_ok:
        print(f"✗ docker daemon not reachable{' (socket ' + funnet + ')' if funnet else ''}. Integration and flyt-tests "
              "need Testcontainers; only unit tests can run.\n  Colima: export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="
              "$HOME/.colima/docker.sock; export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE; "
              "export TESTCONTAINERS_RYUK_DISABLED=true")
        ok = False
    return ok


# ----------------------------------------------------------------------------------------------- main

def finn(indeks: List[TestKlasse], navn: str, modul: Optional[str]) -> List[TestKlasse]:
    kandidater = [k for k in indeks if modul is None or k.modul == modul]
    if "." in navn:
        treff = [k for k in kandidater if k.fqcn == navn]
        if treff:
            return treff
        navn = navn.rsplit(".", 1)[-1]
    for pred in (lambda k: k.navn == navn, lambda k: k.navn.lower() == navn.lower(), lambda k: k.fil.stem == navn):
        treff = [k for k in kandidater if pred(k)]
        if treff:
            return treff
    return []


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("klasser", nargs="*", help="test class names (simple or fully qualified)")
    ap.add_argument("-m", "--metode", help="(part of) the test method name")
    ap.add_argument("--modul", choices=MODULES)
    ap.add_argument("--run", nargs="?", const=0, type=int, metavar="N",
                    help="run the command (or command no. N when several are printed)")
    ap.add_argument("--rerun", action="store_true", help="add Gradle --rerun (ignore up-to-date/build cache)")
    ap.add_argument("--maks-klasser", type=int, metavar="N", help="max classes per command (default flyt 3)")
    ap.add_argument("--maks-tester", type=int, metavar="N", help="max test invocations per command (default flyt 20)")
    ap.add_argument("--liste", choices=ARTER + ("alle",))
    ap.add_argument("--batch", choices=ARTER)
    ap.add_argument("--endret", nargs="?", const="", metavar="REF")
    ap.add_argument("--resultat", nargs="*", metavar="MODUL")
    ap.add_argument("--dsl", nargs="?", const="", metavar="TEKST")
    ap.add_argument("--sjekk-miljo", action="store_true")
    a = ap.parse_args()
    if a.maks_klasser or a.maks_tester:
        for art, (kl, t) in list(GRENSER.items()):
            GRENSER[art] = (a.maks_klasser or kl, a.maks_tester or t)

    if a.sjekk_miljo:
        sys.exit(0 if sjekk_miljo() else 1)
    if a.resultat is not None:
        sys.exit(1 if resultat(a.resultat) else 0)
    if a.dsl is not None:
        dsl(a.dsl or None)
        return

    indeks = indekser()
    valgte: List[TestKlasse] = []
    komprimer_pakker = False

    if a.liste:
        if a.liste == "alle" and not a.modul:
            tabell: Dict[Tuple[str, str], List[TestKlasse]] = defaultdict(list)
            for k in indeks:
                tabell[(k.modul, k.art)].append(k)
            print(f"{'module':<16}{'kind':<13}{'classes':>8}{'tests':>8}")
            for (modul, art), ks in sorted(tabell.items()):
                print(f"{modul:<16}{art:<13}{len(ks):>8}{sum(k.kost for k in ks):>8}")
            print("# list one kind or module: --liste flyt | --liste enhet --modul api")
            return
        ks = [k for k in indeks if (a.liste == "alle" or k.art == a.liste) and (not a.modul or k.modul == a.modul)]
        for k in sorted(ks, key=lambda k: (k.modul, k.art, k.fqcn)):
            ganger = f"{k.tester}x{k.faktor}" if k.faktor > 1 else str(k.tester)
            uten_docker = k.art == "integrasjon" and not k.docker
            print(f"{k.art:<12}{k.modul:<16}{ganger:>6}  {k.fqcn}{'   (no docker)' if uten_docker else ''}")
        print(f"# {len(ks)} classes, {sum(k.kost for k in ks)} test invocations")
        return

    if a.batch:
        valgte = [k for k in indeks if k.art == a.batch and (not a.modul or k.modul == a.modul)]
        komprimer_pakker = True
    elif a.endret is not None:
        valgte = endret(a.endret or None, indeks)
        if not valgte:
            return
        print("\nCommands for the direct matches:")
    elif a.klasser:
        for navn in a.klasser:
            treff = finn(indeks, navn, a.modul)
            if not treff:
                forslag = [k.fqcn for k in indeks if navn.lower() in k.fqcn.lower()][:10]
                sys.exit(f"No test class '{navn}'." + ("\nDid you mean:\n  " + "\n  ".join(forslag) if forslag else ""))
            if len(treff) > 1:
                print(f"# '{navn}' matches {len(treff)} classes (use the FQCN or --modul to pick one): "
                      + ", ".join(f"{k.modul}:{k.fqcn}" for k in treff))
            valgte += treff
    else:
        ap.print_help()
        return

    valgte = list(OrderedDict((k.fqcn + k.modul, k) for k in valgte).values())
    kommandoer = lag_kommandoer(valgte, indeks, a.metode, a.rerun, komprimer_pakker)
    if a.run is None:
        skriv_kommandoer(kommandoer)
        if any(k.docker for k in valgte):
            print("# needs Docker (Testcontainers). Check: kjor_test.py --sjekk-miljo")
        return
    if a.run == 0 and len(kommandoer) > 1:
        skriv_kommandoer(kommandoer)
        sys.exit("Several commands: run one per tool call with --run N.")
    nr = a.run or 1
    if not 1 <= nr <= len(kommandoer):
        sys.exit(f"--run {nr}: there are {len(kommandoer)} commands")
    modul, _, cmd = kommandoer[nr - 1]
    if len(kommandoer) > 1:
        print(f"# command {nr} of {len(kommandoer)}")
    sys.exit(kjør(cmd, [k for k in valgte if k.modul == modul]))


if __name__ == "__main__":
    main()
