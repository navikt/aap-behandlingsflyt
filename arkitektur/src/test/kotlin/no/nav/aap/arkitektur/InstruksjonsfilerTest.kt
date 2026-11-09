package no.nav.aap.arkitektur

import no.nav.aap.arkitektur.Kildekode.prosjekt
import no.nav.aap.arkitektur.Kildekode.prosjektrot
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.FileSystems
import kotlin.io.path.Path

/**
 * AGENTS.md, instruksjonsfilene og skillene i `.github/` peker på filer, klasser og skills med `backticks`.
 * Pekerne kompilerer ikke, så de råtner stille når noe flyttes eller døpes om, og agenten følger dem likevel.
 * Denne testen gjør pekerne til noe som feiler bygget: det som pekes på skal finnes. Om beskrivelsen rundt
 * fortsatt stemmer, må fortsatt et menneske (eller en review) avgjøre.
 *
 * Konvensjoner testen bygger på: mapper skrives med skråstrek til slutt (`kontrakt/`), `...` i en sti står for
 * vilkårlige mellomledd (`repository/.../PostgresRepositoryRegistry.kt`), og maler med `Xxx` eller `<slikt>` sjekkes ikke.
 * Et kulepunkt om et naborepo (`../`) sjekkes ikke, så pekere til egen kode skal stå i et eget kulepunkt.
 */
class InstruksjonsfilerTest {

    private data class Peker(val fil: String, val linje: Int, val tekst: String) {
        override fun toString() = "$fil:$linje: `$tekst`"
    }

    private val instruksjonsfiler: List<File> = listOf(File(prosjektrot, "AGENTS.md")) +
        File(prosjektrot, ".github").walkTopDown().filter { it.isFile && it.extension == "md" }.sorted()

    /**
     * Alt i backticks utenfor kodeblokker. Kodeblokker er illustrasjon, backticks i løpende tekst er pekere.
     * Avsnitt om naborepoer (`../`) peker på kode som ikke finnes her, og hoppes over, med innrykkede
     * fortsettelseslinjer.
     */
    private val pekere: List<Peker> = instruksjonsfiler.flatMap { fil ->
        var iKodeblokk = false
        var iNaborepo = false
        fil.readLines().flatMapIndexed { indeks, linje ->
            val erFortsettelse = linje.startsWith(" ") && !linje.trimStart().startsWith("- ")
            if (!erFortsettelse) iNaborepo = "../" in linje
            when {
                linje.trimStart().startsWith("```") -> { iKodeblokk = !iKodeblokk; emptyList() }
                iKodeblokk || iNaborepo -> emptyList()
                else -> Regex("`([^`]+)`").findAll(linje)
                    .map { Peker(fil.relativeTo(prosjektrot).invariantSeparatorsPath, indeks + 1, it.groupValues[1].trim()) }
                    .toList()
            }
        }
    }

    /** Alle filer og mapper i repoet, relativt til roten. Mapper slutter med `/`. */
    private val stier: List<String> by lazy {
        prosjektrot.walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", ".idea", "node_modules") }
            .drop(1)
            .map { it.relativeTo(prosjektrot).invariantSeparatorsPath + if (it.isDirectory) "/" else "" }
            .toList()
    }

    private fun erMal(tekst: String) = listOf("Xxx", "xxx", "<", ">", "{", "$", " ").any { it in tekst }

    // --- Stier ------------------------------------------------------------------------------------------------------

    private fun erSti(tekst: String) =
        !erMal(tekst) &&
            Regex("""^[\p{L}\p{N}_./*…-]+$""").matches(tekst) &&
            !tekst.startsWith(".") && !tekst.startsWith("/") && !tekst.startsWith("build/") &&
            (tekst.endsWith("/") || tekst.contains("/") && tekst.contains(".") || tekst.endsWith(".kt") || tekst.endsWith(".sql"))

    private fun stiFinnes(tekst: String): Boolean {
        val mønster = Regex("""\.\.\.|\*|[^.*]+|\.""").findAll(tekst.removeSuffix("/").replace("…", "..."))
            .joinToString("") {
                when (it.value) {
                    "..." -> ".*"
                    "*" -> "[^/]*"
                    else -> Regex.escape(it.value)
                }
            }
        val regex = Regex("""^(.*/)?$mønster/?$""")
        return stier.any { regex.matches(it) }
    }

    @Test
    fun `alle filstier i instruksjonsfilene finnes`() {
        val stipekere = pekere.filter { erSti(it.tekst) }
        sjekkRegel(
            sjekket = stipekere,
            funn = stipekere.filterNot { stiFinnes(it.tekst) }.map { it.toString() },
            slikRetterDu = "Filen eller mappen er flyttet, omdøpt eller slettet. Rett pekeren i instruksjonsfilen, " +
                "og les avsnittet rundt: beskrivelsen kan også ha blitt feil.",
        )
    }

    // --- Klasser og medlemmer -----------------------------------------------------------------------------------------

    /** `Navn`, `*Suffiks`, `Prefiks*`, `Navn.medlem`, `Navn.medlem()` eller `Navn.medlem*`. Ikke KONSTANTER (SQL-ord). */
    private val typepeker = Regex("""^(\*?)([A-ZÆØÅ][\p{L}\p{N}_]*)(\*?)(?:\.([\p{L}\p{N}_]+)(\*?)(?:\(\))?)?$""")

    private fun somTypepeker(tekst: String): MatchResult? =
        if (erMal(tekst) || "…" in tekst || erSti(tekst)) null
        else typepeker.matchEntire(tekst)?.takeIf { m -> m.groupValues[2].any { it.isLowerCase() } }

    /**
     * Navn koden kjenner: egne deklarasjoner (også i testkode), filnavn (`FlytApi` er en fil med toppnivåfunksjoner)
     * og alt som importeres fra biblioteker.
     */
    private val kjenteNavn: Set<String> by lazy {
        val klasser = prosjekt.classes(includeNested = true)
        (klasser.map { it.name } +
            prosjekt.files.map { it.name.removeSuffix(".kt") } +
            klasser.flatMap { k -> k.enumConstants.map { it.name } } +
            prosjekt.objects(includeNested = true).map { it.name } +
            prosjekt.interfaces(includeNested = true).map { it.name } +
            prosjekt.typeAliases.map { it.name } +
            prosjekt.imports.map { it.name.substringAfterLast('.') }).toSet()
    }

    private fun navnFinnes(prefiks: String, navn: String, suffiks: String): Boolean = when {
        prefiks.isEmpty() && suffiks.isEmpty() -> navn in kjenteNavn
        else -> {
            val regex = Regex("^" + (if (prefiks.isNotEmpty()) ".*" else "") + Regex.escape(navn) + (if (suffiks.isNotEmpty()) ".*" else "") + "$")
            kjenteNavn.any { regex.matches(it) }
        }
    }

    private fun medlemFinnes(type: String, medlem: String, prefiks: Boolean): Boolean {
        val egneFiler = prosjekt.files.filter { f ->
            f.classes(includeNested = true).any { it.name == type } ||
                f.objects(includeNested = true).any { it.name == type } ||
                f.interfaces(includeNested = true).any { it.name == type }
        }
        val tekst = (egneFiler.ifEmpty { prosjekt.files }).joinToString("\n") { it.text }
        val regex = if (prefiks) Regex("""(?<![\p{L}\p{N}_])${Regex.escape(medlem)}""") else helOrd(medlem)
        return regex.containsMatchIn(tekst)
    }

    @Test
    fun `alle klassenavn i instruksjonsfilene finnes i koden`() {
        val typepekere = pekere.mapNotNull { p -> somTypepeker(p.tekst)?.let { p to it } }
        val funn = typepekere.filterNot { (_, m) ->
            val (prefiks, navn, suffiks, medlem, medlemPrefiks) = m.destructured
            navnFinnes(prefiks, navn, suffiks) &&
                (medlem.isEmpty() || medlemFinnes(navn, medlem, medlemPrefiks.isNotEmpty()))
        }
        sjekkRegel(
            sjekket = typepekere,
            funn = funn.map { (p, _) -> p.toString() },
            slikRetterDu = "Klassen, konstanten eller medlemmet finnes ikke lenger under det navnet (verken deklarert eller " +
                "importert). Rett pekeren i instruksjonsfilen. Er ordet prosa og ikke kode, fjern backticks.",
        )
    }

    // --- Skills, agenter og andre navn med bindestrek ---------------------------------------------------------------

    private val skillkommando = Regex("""^/([a-z][a-z-]*)(\s.*)?$""")
    private val bindestreknavn = Regex("""^[a-z]+(-[a-z]+)+$""")

    private fun basenavnFinnes(navn: String) = stier.any { sti ->
        val basenavn = sti.removeSuffix("/").substringAfterLast('/')
        basenavn == navn || basenavn == "$navn.md" || basenavn == "$navn.agent.md"
    }

    /** `/navn` er en skill, eller en HTTP-rute som finnes i en strengliteral i koden (`route("/{referanse}/forbered")`). */
    private fun kommandoFinnes(navn: String): Boolean {
        val rute = Regex("/" + Regex.escape(navn) + """(?![\p{L}\p{N}_-])""")
        return ".github/skills/$navn/" in stier || prosjekt.files.any { rute.containsMatchIn(it.text) }
    }

    @Test
    fun `alle skills og navn med bindestrek i instruksjonsfilene finnes`() {
        val kommandoer = pekere.mapNotNull { p -> skillkommando.matchEntire(p.tekst)?.let { p to it.groupValues[1] } }
        val navn = pekere.filter { bindestreknavn.matches(it.tekst) }
        sjekkRegel(
            sjekket = kommandoer + navn,
            funn = kommandoer.filterNot { (_, k) -> kommandoFinnes(k) }.map { (p, _) -> p.toString() } +
                navn.filterNot { basenavnFinnes(it.tekst) }.map { it.toString() },
            slikRetterDu = "Skillen, agenten eller mappen finnes ikke under det navnet. Rett pekeren, eller opprett det som mangler.",
        )
    }

    @Test
    fun `alle skills er nevnt i AGENTS_md`() {
        val skills = File(prosjektrot, ".github/skills").listFiles { f -> f.isDirectory }!!.map { it.name }.sorted()
        val agentsMd = File(prosjektrot, "AGENTS.md").readText()
        sjekkRegel(
            sjekket = skills,
            funn = skills.filterNot { helOrd(it).containsMatchIn(agentsMd) },
            slikRetterDu = "Nevn skillen i AGENTS.md, ellers vet ikke agenten at oppskriften finnes før den tilfeldigvis matcher.",
        )
    }

    // --- applyTo ----------------------------------------------------------------------------------------------------

    @Test
    fun `alle applyTo-globs i instruksjonsfilene treffer minst én fil`() {
        val globs = instruksjonsfiler.filter { it.name.endsWith(".instructions.md") }.flatMap { fil ->
            val frontmatter = fil.readText().substringAfter("---").substringBefore("---")
            Regex("""applyTo:\s*"([^"]+)"""").find(frontmatter)?.groupValues?.get(1)?.split(",")
                ?.map { Peker(fil.relativeTo(prosjektrot).invariantSeparatorsPath, 0, it.trim()) }
                ?: listOf(Peker(fil.relativeTo(prosjektrot).invariantSeparatorsPath, 0, "<mangler applyTo>"))
        }
        val filer = stier.filterNot { it.endsWith("/") }.map { Path(it) }
        sjekkRegel(
            sjekket = globs,
            funn = globs.filterNot { g ->
                g.tekst.startsWith("<").not() &&
                    FileSystems.getDefault().getPathMatcher("glob:${g.tekst}").let { m -> filer.any { m.matches(it) } }
            }.map { it.toString() },
            slikRetterDu = "Globen i applyTo treffer ingen fil, så instruksjonen lastes aldri. Rett globen (eller legg til applyTo).",
        )
    }
}
