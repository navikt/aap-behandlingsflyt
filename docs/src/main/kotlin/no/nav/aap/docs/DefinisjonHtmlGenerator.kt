package no.nav.aap.docs

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import java.io.File

fun main(args: Array<String>) {
    val outputPath = args.firstOrNull() ?: error("Mangler output-filsti som argument")
    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()
    outputFile.writeText(genererHtml())
    println("Genererte ${outputFile.absolutePath}")
}

private fun erDeprecated(definisjon: Definisjon): Boolean =
    Definisjon::class.java
        .getField(definisjon.name)
        .isAnnotationPresent(Deprecated::class.java)

private fun genererHtml(): String {
    val rader = Definisjon.entries.joinToString("\n") { definisjon ->
        val deprecated = erDeprecated(definisjon)
        val rowClass = if (deprecated) " class=\"deprecated\"" else ""
        val roller = definisjon.løsesAv.joinToString(", ") { it.name }
        val stegGruppe = definisjon.løsesISteg.gruppe.name
        """        <tr$rowClass>
          <td>${definisjon.name}</td>
          <td>${definisjon.kode.name}</td>
          <td>${definisjon.type.name}</td>
          <td>${definisjon.løsesISteg.name}</td>
          <td>$stegGruppe</td>
          <td>${if (definisjon.kreverToTrinn) "✓" else ""}</td>
          <td>$roller</td>
        </tr>"""
    }

    return """<!DOCTYPE html>
<html lang="no">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Avklaringsbehov – oversikt</title>
  <style>
    body { font-family: sans-serif; margin: 2rem; color: #333; }
    h1 { margin-bottom: 0.5rem; }
    p.subtitle { margin-top: 0; color: #666; }
    #filter { margin-bottom: 1rem; padding: 0.4rem 0.6rem; font-size: 1rem; width: 100%; max-width: 400px; border: 1px solid #ccc; border-radius: 4px; }
    table { border-collapse: collapse; width: 100%; font-size: 0.9rem; table-layout: fixed; word-break: break-word; }
    th { background: #1a73e8; color: #fff; padding: 0.5rem 0.75rem; cursor: pointer; user-select: none; }
    col.col-navn   { width: 22%; }
    col.col-kode   { width: 6%; }
    col.col-type   { width: 14%; }
    col.col-steg   { width: 18%; }
    col.col-gruppe { width: 12%; }
    col.col-totrinn { width: 7%; }
    col.col-roller { width: 21%; }
    th:hover { background: #1558b0; }
    th.asc::after { content: " ▲"; }
    th.desc::after { content: " ▼"; }
    td { padding: 0.4rem 0.75rem; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
    tr:nth-child(even) { background: #f8f8f8; }
    tr.deprecated td { text-decoration: line-through; color: #999; }
    tr:hover { background: #e8f0fe; }
    .hidden { display: none; }
    #count { margin-top: 0.5rem; font-size: 0.85rem; color: #555; }
  </style>
</head>
<body>
  <h1>Avklaringsbehov – oversikt</h1>
  <p class="subtitle">Alle definisjoner i <code>Definisjon.kt</code>. Rader med gjennomstreking er markert <code>@Deprecated</code>.</p>
  <input id="filter" type="search" placeholder="Filtrer…" oninput="filterTabell(this.value)">
  <div id="count"></div>
  <table id="tabell">
    <colgroup>
      <col class="col-navn"><col class="col-kode"><col class="col-type">
      <col class="col-steg"><col class="col-gruppe"><col class="col-totrinn"><col class="col-roller">
    </colgroup>
    <thead>
      <tr>
        <th onclick="sorterKolonne(0)">Navn</th>
        <th onclick="sorterKolonne(1)">Kode</th>
        <th onclick="sorterKolonne(2)">Type</th>
        <th onclick="sorterKolonne(3)">løsesISteg</th>
        <th onclick="sorterKolonne(4)">StegGruppe</th>
        <th onclick="sorterKolonne(5)">kreverToTrinn</th>
        <th onclick="sorterKolonne(6)">Roller (løsesAv)</th>
      </tr>
    </thead>
    <tbody>
$rader
    </tbody>
  </table>
  <script>
    var sortertKolonne = -1;
    var stigende = true;

    function oppdaterAntall() {
      var synlige = document.querySelectorAll('#tabell tbody tr:not(.hidden)').length;
      var totalt = document.querySelectorAll('#tabell tbody tr').length;
      document.getElementById('count').textContent = synlige + ' av ' + totalt + ' oppføringer';
    }

    function filterTabell(tekst) {
      var filter = tekst.toLowerCase();
      var rader = document.querySelectorAll('#tabell tbody tr');
      rader.forEach(function(rad) {
        var innhold = rad.textContent.toLowerCase();
        rad.classList.toggle('hidden', filter.length > 0 && !innhold.includes(filter));
      });
      oppdaterAntall();
    }

    function sorterKolonne(kolIdx) {
      var tabell = document.getElementById('tabell');
      var tbody = tabell.querySelector('tbody');
      var rader = Array.from(tbody.querySelectorAll('tr'));
      var headers = tabell.querySelectorAll('th');

      if (sortertKolonne === kolIdx) {
        stigende = !stigende;
      } else {
        stigende = true;
        sortertKolonne = kolIdx;
      }

      headers.forEach(function(h, i) {
        h.classList.remove('asc', 'desc');
        if (i === kolIdx) h.classList.add(stigende ? 'asc' : 'desc');
      });

      rader.sort(function(a, b) {
        var aVal = (a.cells[kolIdx] ? a.cells[kolIdx].textContent : '').trim();
        var bVal = (b.cells[kolIdx] ? b.cells[kolIdx].textContent : '').trim();
        return stigende ? aVal.localeCompare(bVal, 'no') : bVal.localeCompare(aVal, 'no');
      });

      rader.forEach(function(rad) { tbody.appendChild(rad); });
      oppdaterAntall();
    }

    oppdaterAntall();
  </script>
</body>
</html>"""
}
