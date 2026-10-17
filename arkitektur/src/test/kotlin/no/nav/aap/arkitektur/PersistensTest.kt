package no.nav.aap.arkitektur

import no.nav.aap.arkitektur.Kildekode.produksjon
import no.nav.aap.arkitektur.Kildekode.prosjektrot
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Grunnlag oppdateres aldri in-place: mønsteret er deaktiver (`aktiv = false`) + insert ny rad per behandling_id.
 * Ny behandling får pekerkopier av forrige behandlings aktive rader, så en UPDATE på en slik rad endrer også
 * historiske behandlinger. Regelen leser SQL-strenger i koden, så dynamisk bygde tabellnavn sjekkes ikke.
 */
class PersistensTest {

    private fun normaliser(sql: String) = sql.replace(Regex("""\s+"""), " ").trim()

    @Test
    fun `tabeller med aktiv-flagg oppdateres bare ved å deaktivere`() {
        val migreringer = File(prosjektrot, "repository/src/main/resources/flyway").listFiles()!!
            .filter { it.extension == "sql" }.joinToString("\n") { it.readText() }
        val opprettetMedAktiv = Regex("""(?is)CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(\w+)\s*\((.*?)\);""")
            .findAll(migreringer).filter { Regex("""(?i)\baktiv\b""").containsMatchIn(it.groupValues[2]) }
            .map { it.groupValues[1] }
        val fikkAktivSenere = Regex("""(?is)ALTER\s+TABLE\s+(?:IF\s+EXISTS\s+)?(\w+)\s+ADD\s+(?:COLUMN\s+)?(?:IF\s+NOT\s+EXISTS\s+)?aktiv\b""")
            .findAll(migreringer).map { it.groupValues[1] }
        val aktivTabeller = (opprettetMedAktiv + fikkAktivSenere).map { it.lowercase() }.toSet()

        val oppdateringer = produksjon.files.flatMap { f ->
            Regex("""(?is)\bUPDATE\s+(\w+)\s+SET\s+(.{0,200}?)(?=\bWHERE\b|"{3})""").findAll(f.text)
                .map { Triple(f.name, it.groupValues[1], normaliser(it.groupValues[2])) }.toList()
        }.filter { (_, tabell, _) -> tabell.lowercase() in aktivTabeller }

        val bareDeaktivering = Regex("""(?i)aktiv\s*=\s*(false|'f'|\?)(\s*,\s*\w+\s*=\s*\S+)*\s*,?""")
        sjekkRegel(
            sjekket = oppdateringer,
            funn = oppdateringer.filterNot { (_, _, set) -> bareDeaktivering.matches(set) }
                .map { (fil, tabell, set) -> "$fil: UPDATE $tabell SET $set" },
            slikRetterDu = "Deaktiver gjeldende rad (UPDATE … SET aktiv = false) og sett inn en ny rad i stedet for å " +
                "endre den. Se hard regel 4 i AGENTS.md.",
        )
    }
}
