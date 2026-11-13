package no.nav.aap.arkitektur

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.provider.KoParentProvider
import java.io.File
import kotlin.test.fail

/**
 * Felles scope for arkitekturtestene: alle .kt-filer under `<modul>/src/main`, parset av Konsist (ca. 0,5 s).
 * Prosjektet kompileres ikke, så reglene sjekker kildekodens form og tekst, ikke typer.
 */
object Kildekode {
    val produksjon by lazy { Konsist.scopeFromProduction() }
    /** Også testkode. Brukes der instruksjonsfilene kan peke på testklasser og fakes. */
    val prosjekt by lazy { Konsist.scopeFromProject() }
    val klasser by lazy { produksjon.classes(includeNested = true) }
    val objekter by lazy { produksjon.objects(includeNested = true) }
    val grensesnitt by lazy { produksjon.interfaces(includeNested = true) }

    val prosjektrot: File by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
    }

    fun fil(sti: String): KoFileDeclaration = produksjon.files.single { it.path.endsWith(sti) }

    /** Alle `register<Navn>()` i en registreringsliste. */
    fun registrerte(fil: KoFileDeclaration): Set<String> =
        Regex("""register<($IDENTIFIKATOR)>""").findAll(fil.text).map { it.groupValues[1] }.toSet()
}

/** Kotlin-identifikator med æøå. `\w` i Kotlin/Java-regex matcher bare ASCII. */
const val IDENTIFIKATOR = """[\p{L}\p{N}_]+"""

/** Matcher [ord] som helt ord, også når det inneholder æøå. */
fun helOrd(ord: String) = Regex("""(?<![\p{L}\p{N}_])${Regex.escape(ord)}(?![\p{L}\p{N}_])""")

fun KoParentProvider.arver(vararg navn: String) =
    parents(indirectParents = true).any { it.name.substringBefore("<") in navn }

fun KoParentProvider.foreldrenavn() = parents().map { it.name.substringBefore("<") }

fun KoClassDeclaration.erKonkret() = !hasAbstractModifier && !hasSealedModifier && !hasEnumModifier

/**
 * Sammenligner [funn] med [kjenteBrudd], og virker som en skralle: nye brudd feiler testen, og kjente brudd som er
 * rettet feiler også, til de er fjernet fra listen. Listen over kjente brudd kan derfor bare krympe.
 *
 * Feiler også hvis regelen ikke fant noe å sjekke, slik at en regel som slutter å matche koden ikke består i det stille.
 */
fun sjekkRegel(
    sjekket: Collection<*>,
    funn: Collection<String>,
    kjenteBrudd: Set<String> = emptySet(),
    slikRetterDu: String,
) {
    if (sjekket.isEmpty()) fail("Regelen fant ingenting å sjekke. Har koden endret form? Da må regelen oppdateres.")
    val nye = funn.toSet() - kjenteBrudd
    val rettet = kjenteBrudd - funn.toSet()
    val melding = buildString {
        if (nye.isNotEmpty()) {
            appendLine("${nye.size} nye brudd (av ${sjekket.size} sjekket):")
            nye.sorted().forEach { appendLine("  - $it") }
            appendLine(slikRetterDu)
        }
        if (rettet.isNotEmpty()) {
            appendLine("Disse kjente bruddene er rettet. Fjern dem fra kjenteBrudd i testen:")
            rettet.sorted().forEach { appendLine("  - $it") }
        }
    }
    if (melding.isNotEmpty()) fail(melding)
}
