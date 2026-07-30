@file:Suppress("TooManyFunctions")

package no.nav.aap.docs

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import java.io.File

private data class StegRad(
    val indeks: Int,
    val type: String,
    val klassenavn: String,
    val vurderingsbehov: List<String>,
    val gjelderAlle: Boolean,
    val inkludererGRegulering: Boolean,
)

fun main(args: Array<String>) {
    val outputDirectoryPath = args.firstOrNull() ?: error("Mangler output-katalog som argument")
    val outputDirectory = File(outputDirectoryPath).apply { mkdirs() }
    val outputFile = outputDirectory.resolve("avklaringsbehov.html")

    outputFile.writeText(genererOversiktHtml())
    outputDirectory.resolve("revurdering-steg.html").delete()
    println("Genererte ${outputFile.absolutePath}")
}

private fun genererOversiktHtml(): String {
    val steg = finnRevurderingSteg()
    val stegPerType = steg.associateBy { it.type }
    val alleVurderingsbehov = finnAlleVurderingsbehov()
    val definisjonerPerSteg = Definisjon.entries.groupBy { it.løsesISteg.name }
    val vurderingsbehovPerSteg = steg.associate { rad ->
        rad.type to if (rad.gjelderAlle) {
            alleVurderingsbehov.filter { rad.inkludererGRegulering || it != "G_REGULERING" }
        } else {
            rad.vurderingsbehov
        }
    }

    val definisjonRader = genererDefinisjonRader(stegPerType, vurderingsbehovPerSteg)
    val stegRader = genererStegRader(steg, definisjonerPerSteg, vurderingsbehovPerSteg)
    val vurderingsbehovRader = genererVurderingsbehovRader(
        alleVurderingsbehov,
        steg,
        definisjonerPerSteg,
        vurderingsbehovPerSteg,
    )

    return htmlDokument(definisjonRader, stegRader, vurderingsbehovRader)
}

private fun genererDefinisjonRader(
    stegPerType: Map<String, StegRad>,
    vurderingsbehovPerSteg: Map<String, List<String>>,
): String = Definisjon.entries.joinToString("\n") { definisjon ->
    val deprecatedClass = if (erDeprecated(definisjon)) " deprecated" else ""
    val stegType = definisjon.løsesISteg.name
    val steg = stegPerType[stegType]
    val stegVisning = steg?.klassenavn?.removeSuffix("Steg")?.tilLesbarTittel() ?: stegType.tilLesbarTittel()
    val behov = vurderingsbehovPerSteg[stegType].orEmpty()
    val søkeverdier = listOf(definisjon.name, stegType) + behov

    """        <tr id="${anchor("definisjon", definisjon.name)}" class="data-row$deprecatedClass" data-values="${dataValues(søkeverdier)}">
          <td>${definisjon.name.tilLesbarTittel()}<small>${definisjon.name}</small></td>
          <td>${definisjon.kode.name}</td>
          <td>${definisjon.type.name.tilLesbarTittel()}</td>
          <td>${relasjonslenke("steg", stegType, stegVisning)}</td>
          <td>${chips("vurderingsbehov", behov)}</td>
          <td>${jaNei(definisjon.kreverToTrinn)}</td>
          <td>${jaNei(definisjon.kvalitetssikres)}</td>
          <td>${definisjon.løsesAv.joinToString(", ") { it.name.tilLesbarTittel() }}</td>
        </tr>"""
}

private fun genererStegRader(
    steg: List<StegRad>,
    definisjonerPerSteg: Map<String, List<Definisjon>>,
    vurderingsbehovPerSteg: Map<String, List<String>>,
): String = steg.joinToString("\n") { rad ->
    val definisjoner = definisjonerPerSteg[rad.type].orEmpty()
    val behov = vurderingsbehovPerSteg[rad.type].orEmpty()
    val søkeverdier = listOf(rad.type, rad.klassenavn) + definisjoner.map { it.name } + behov

    """        <tr id="${anchor("steg", rad.type)}" class="data-row" data-values="${dataValues(søkeverdier)}">
          <td>${rad.indeks}</td>
          <td>${rad.klassenavn.removeSuffix("Steg").tilLesbarTittel()}<small>${rad.type}</small></td>
          <td>${chipsForDefinisjoner(definisjoner)}</td>
          <td>${chips("vurderingsbehov", behov)}</td>
        </tr>"""
}

private fun genererVurderingsbehovRader(
    alleVurderingsbehov: List<String>,
    steg: List<StegRad>,
    definisjonerPerSteg: Map<String, List<Definisjon>>,
    vurderingsbehovPerSteg: Map<String, List<String>>,
): String = alleVurderingsbehov.joinToString("\n") { behov ->
    val relevanteSteg = steg.filter { behov in vurderingsbehovPerSteg[it.type].orEmpty() }
    val definisjoner = relevanteSteg.flatMap { definisjonerPerSteg[it.type].orEmpty() }.distinct()
    val søkeverdier = listOf(behov) + relevanteSteg.map { it.type } + definisjoner.map { it.name }

    """        <tr id="${anchor("vurderingsbehov", behov)}" class="data-row" data-values="${dataValues(søkeverdier)}">
          <td>${behov.tilLesbarTittel()}<small>$behov</small></td>
          <td>${relevanteSteg.joinToString(" ") { relasjonslenke("steg", it.type, it.klassenavn.removeSuffix("Steg").tilLesbarTittel()) }}</td>
          <td>${chipsForDefinisjoner(definisjoner)}</td>
        </tr>"""
}

@Suppress("LongMethod")
private fun htmlDokument(
    definisjonRader: String,
    stegRader: String,
    vurderingsbehovRader: String,
): String = """<!DOCTYPE html>
<html lang="no">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Behandlingsflyt – samlet oversikt</title>
  <style>
    :root { --blue: #0056b4; --light-blue: #e6f0ff; --border: #d8d8d8; --text-muted: #666; }
    * { box-sizing: border-box; }
    body { font-family: sans-serif; margin: 0; color: #262626; background: #f7f7f7; }
    header { position: sticky; top: 0; z-index: 10; padding: 1.25rem 2rem 1rem; background: white; border-bottom: 1px solid var(--border); }
    h1 { margin: 0 0 0.35rem; font-size: 1.6rem; }
    .subtitle { margin: 0 0 1rem; color: var(--text-muted); }
    .controls { display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap; }
    #filter { padding: 0.55rem 0.7rem; font-size: 1rem; width: min(32rem, 100%); border: 1px solid #888; border-radius: 4px; }
    button { padding: 0.55rem 0.8rem; border: 1px solid var(--blue); border-radius: 4px; background: white; color: var(--blue); cursor: pointer; }
    nav a { margin-right: 1rem; color: var(--blue); font-weight: 600; }
    main { padding: 1.5rem 2rem 4rem; max-width: 1800px; margin: auto; }
    section { margin: 0 0 3rem; scroll-margin-top: 10rem; }
    h2 { margin-bottom: 0.35rem; }
    .section-description, .count { color: var(--text-muted); font-size: 0.9rem; }
    .table-wrapper { overflow-x: auto; margin-top: 0.75rem; background: white; border: 1px solid var(--border); border-radius: 5px; }
    table { border-collapse: collapse; width: 100%; font-size: 0.9rem; }
    th { position: sticky; top: 0; background: var(--blue); color: white; text-align: left; padding: 0.65rem 0.75rem; cursor: pointer; white-space: nowrap; }
    th:hover { background: #003f85; }
    th.asc::after { content: " ▲"; }
    th.desc::after { content: " ▼"; }
    td { padding: 0.55rem 0.75rem; border-bottom: 1px solid #e5e5e5; vertical-align: top; }
    tr:nth-child(even) { background: #fafafa; }
    tr:hover td, tr:target td { background: var(--light-blue); }
    tr.deprecated { color: #888; text-decoration: line-through; }
    small { display: block; margin-top: 0.2rem; color: var(--text-muted); font-family: monospace; font-size: 0.75rem; }
    .chip { display: inline-block; margin: 0.1rem 0.2rem 0.1rem 0; padding: 0.18rem 0.42rem; border-radius: 999px; background: var(--light-blue); color: #003f85; text-decoration: none; font-size: 0.8rem; }
    .chip:hover { background: #c9ddf8; text-decoration: underline; }
    .hidden { display: none; }
    .empty { color: var(--text-muted); font-style: italic; }
    @media (max-width: 700px) { header, main { padding-left: 1rem; padding-right: 1rem; } }
  </style>
</head>
<body>
  <header>
    <h1>Behandlingsflyt – samlet oversikt</h1>
    <p class="subtitle">Sammenhengen mellom avklaringsbehov, steg i revurdering og vurderingsbehov.</p>
    <div class="controls">
      <input id="filter" type="search" placeholder="Søk eller klikk på en relasjon…" oninput="filtrer(this.value)">
      <button type="button" onclick="nullstill()">Nullstill</button>
      <nav><a href="#definisjoner">Avklaringsbehov</a><a href="#steg">Steg</a><a href="#vurderingsbehov">Vurderingsbehov</a></nav>
    </div>
  </header>
  <main>
    <section id="definisjoner">
      <h2>Avklaringsbehov</h2>
      <div class="section-description">Definisjoner fra <code>Definisjon.kt</code>, steget de løses i og vurderingsbehov som gjør steget relevant.</div>
      <div class="count"></div>
      <div class="table-wrapper"><table>
        <thead><tr>
          <th onclick="sorter(this, 0)">Navn</th><th onclick="sorter(this, 1)">Kode</th>
          <th onclick="sorter(this, 2)">Type</th><th onclick="sorter(this, 3)">Løses i steg</th>
          <th onclick="sorter(this, 4)">Relevante vurderingsbehov</th>
          <th onclick="sorter(this, 5)">To trinn</th><th onclick="sorter(this, 6)">Kvalitetssikres</th>
          <th onclick="sorter(this, 7)">Roller</th>
        </tr></thead><tbody>
$definisjonRader
        </tbody>
      </table></div>
    </section>
    <section id="steg">
      <h2>Steg i revurdering</h2>
      <div class="section-description">Stegene i kjørerekkefølge, med avklaringsbehov som løses der og vurderingsbehov som gjør steget relevant.</div>
      <div class="count"></div>
      <div class="table-wrapper"><table>
        <thead><tr>
          <th onclick="sorter(this, 0)">#</th><th onclick="sorter(this, 1)">Steg</th>
          <th onclick="sorter(this, 2)">Avklaringsbehov</th><th onclick="sorter(this, 3)">Relevante vurderingsbehov</th>
        </tr></thead><tbody>
$stegRader
        </tbody>
      </table></div>
    </section>
    <section id="vurderingsbehov">
      <h2>Vurderingsbehov</h2>
      <div class="section-description">Alle vurderingsbehov, stegene de utløser i revurderingsflyten og avklaringsbehov som finnes i disse stegene.</div>
      <div class="count"></div>
      <div class="table-wrapper"><table>
        <thead><tr>
          <th onclick="sorter(this, 0)">Vurderingsbehov</th><th onclick="sorter(this, 1)">Relevante steg</th>
          <th onclick="sorter(this, 2)">Avklaringsbehov i stegene</th>
        </tr></thead><tbody>
$vurderingsbehovRader
        </tbody>
      </table></div>
    </section>
  </main>
  <script>
    function normaliser(verdi) {
      return verdi.toLocaleLowerCase('no').replaceAll('_', ' ').trim();
    }

    function filtrer(verdi) {
      var filter = normaliser(verdi);
      document.querySelectorAll('section').forEach(function(section) {
        var rader = section.querySelectorAll('tbody .data-row');
        var synlige = 0;
        rader.forEach(function(rad) {
          var innhold = normaliser(rad.textContent + ' ' + (rad.dataset.values || ''));
          var vis = filter.length === 0 || innhold.includes(filter);
          rad.classList.toggle('hidden', !vis);
          if (vis) synlige++;
        });
        section.querySelector('.count').textContent = synlige + ' av ' + rader.length + ' oppføringer';
      });
    }

    function velgRelasjon(event, verdi) {
      event.preventDefault();
      document.getElementById('filter').value = verdi.replaceAll('_', ' ');
      filtrer(verdi);
      window.history.replaceState(null, '', '#' + event.currentTarget.dataset.anchor);
    }

    function nullstill() {
      document.getElementById('filter').value = '';
      filtrer('');
      window.history.replaceState(null, '', window.location.pathname);
    }

    function sammenlign(a, b) {
      var aNum = Number(a);
      var bNum = Number(b);
      if (a.length > 0 && b.length > 0 && !Number.isNaN(aNum) && !Number.isNaN(bNum)) return aNum - bNum;
      return a.localeCompare(b, 'no');
    }

    function sorter(header, kolonne) {
      var tabell = header.closest('table');
      var tbody = tabell.querySelector('tbody');
      var rader = Array.from(tbody.querySelectorAll('tr'));
      var stigende = !header.classList.contains('asc');
      tabell.querySelectorAll('th').forEach(function(th) { th.classList.remove('asc', 'desc'); });
      header.classList.add(stigende ? 'asc' : 'desc');
      rader.sort(function(a, b) {
        var aVal = a.cells[kolonne].textContent.trim();
        var bVal = b.cells[kolonne].textContent.trim();
        return stigende ? sammenlign(aVal, bVal) : sammenlign(bVal, aVal);
      });
      rader.forEach(function(rad) { tbody.appendChild(rad); });
    }

    filtrer('');
  </script>
</body>
</html>"""

private fun relasjonslenke(type: String, verdi: String, tekst: String): String {
    val id = anchor(type, verdi)
    return """<a class="chip" href="#$id" data-anchor="$id" onclick="velgRelasjon(event, '${jsEscape(verdi)}')">${htmlEscape(tekst)}</a>"""
}

private fun chips(type: String, verdier: List<String>): String =
    if (verdier.isEmpty()) {
        """<span class="empty">Ingen</span>"""
    } else {
        verdier.joinToString(" ") { relasjonslenke(type, it, it.tilLesbarTittel()) }
    }

private fun chipsForDefinisjoner(definisjoner: List<Definisjon>): String =
    if (definisjoner.isEmpty()) {
        """<span class="empty">Ingen</span>"""
    } else {
        definisjoner.joinToString(" ") { relasjonslenke("definisjon", it.name, it.name.tilLesbarTittel()) }
    }

private fun finnRevurderingSteg(): List<StegRad> {
    val sourceText = requireNotNull(
        finnKildeFil(
            "behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/behandlingstyper/Revurdering.kt"
        )
    ).readText()

    return finnMedStegBlokker(sourceText).mapIndexedNotNull { indeks, block ->
        val klassenavn = Regex("""\bsteg\s*=\s*([\p{L}\p{N}_]+)""").find(block)?.groupValues?.get(1)
            ?: return@mapIndexedNotNull null
        val expression = hentArgumentExpression(block, "vurderingsbehovRelevanteForSteg")
            ?: return@mapIndexedNotNull null
        val stegType = finnStegType(klassenavn) ?: klassenavn.removeSuffix("Steg").toStegTypeNavn()
        val vurderingsbehov = Regex("""Vurderingsbehov\.([\p{L}0-9_]+)""")
            .findAll(expression)
            .map { it.groupValues[1] }
            .toList()

        StegRad(
            indeks = indeks + 1,
            type = stegType,
            klassenavn = klassenavn,
            vurderingsbehov = vurderingsbehov,
            gjelderAlle = expression.contains("Vurderingsbehov.alle"),
            inkludererGRegulering = expression.contains("alleInklusivGRegulering"),
        )
    }
}

private fun finnStegType(klassenavn: String): String? {
    val file = finnKildeFil(
        "behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/forretningsflyt/steg/$klassenavn.kt",
        required = false,
    ) ?: return null
    return Regex("""StegType\.([\p{L}0-9_]+)""").find(file.readText())?.groupValues?.get(1)
}

private fun finnAlleVurderingsbehov(): List<String> {
    val text = requireNotNull(
        finnKildeFil(
            "behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/sakogbehandling/flyt/Vurderingsbehov.kt"
        )
    ).readText()
    val enumBody = text.substringAfter("enum class Vurderingsbehov {").substringBefore("companion object")

    return enumBody.lines()
        .map { it.substringBefore("//").trim() }
        .filter { it.matches(Regex("""[\p{L}0-9_]+,?""")) }
        .map { it.removeSuffix(",") }
        .filterNot { it.isBlank() }
}

private fun finnKildeFil(relativePath: String, required: Boolean = true): File? {
    val candidates = listOf(File(relativePath), File("../$relativePath"))
    val file = candidates.firstOrNull { it.exists() }
    if (file == null && required) error("Fant ikke $relativePath i forventede plasseringer")
    return file
}

private fun finnMedStegBlokker(text: String): List<String> {
    val blocks = mutableListOf<String>()
    var searchIndex = 0
    while (searchIndex < text.length) {
        val medStegIndex = text.indexOf(".medSteg(", searchIndex)
        if (medStegIndex == -1) return blocks
        val (block, nextIndex) = finnParentesBlokk(text, medStegIndex) ?: return blocks
        blocks.add(block)
        searchIndex = nextIndex
    }
    return blocks
}

private fun finnParentesBlokk(text: String, startIndex: Int): Pair<String, Int>? {
    val start = text.indexOf('(', startIndex)
    if (start == -1) return null
    var depth = 0
    for (index in start until text.length) {
        when (text[index]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return text.substring(start + 1, index) to (index + 1)
            }
        }
    }
    return null
}

private fun hentArgumentExpression(block: String, argumentName: String): String? {
    val argumentStart = block.indexOf(argumentName)
    if (argumentStart == -1) return null
    val assignmentIndex = block.indexOf('=', argumentStart)
    if (assignmentIndex == -1) return null
    var index = assignmentIndex + 1
    while (index < block.length && block[index].isWhitespace()) index++
    val start = index
    var depth = 0
    while (index < block.length) {
        when (block[index]) {
            '(', '[', '{' -> depth++
            ')', ']', '}' -> if (depth > 0) depth--
            ',' -> if (depth == 0) break
        }
        index++
    }
    return block.substring(start, index).trim()
}

private fun erDeprecated(definisjon: Definisjon): Boolean =
    Definisjon::class.java.getField(definisjon.name).isAnnotationPresent(Deprecated::class.java)

private fun jaNei(verdi: Boolean): String = if (verdi) "Ja" else "Nei"

private fun anchor(type: String, verdi: String): String =
    "$type-${verdi.lowercase().replace(Regex("""[^\p{L}0-9_-]"""), "-")}"

private fun dataValues(verdier: List<String>): String =
    htmlEscape(verdier.joinToString(" ") { it.replace("_", " ") })

private fun String.toStegTypeNavn(): String = buildString {
    this@toStegTypeNavn.forEachIndexed { index, char ->
        if (char.isUpperCase() && index > 0 && this@toStegTypeNavn[index - 1].isLowerCase()) append('_')
        append(char.uppercaseChar())
    }
}

private fun String.tilLesbarTittel(): String =
    split("_").joinToString(" ") { token ->
        token.lowercase().replaceFirstChar { it.titlecase() }
    }

private fun htmlEscape(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private fun jsEscape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
