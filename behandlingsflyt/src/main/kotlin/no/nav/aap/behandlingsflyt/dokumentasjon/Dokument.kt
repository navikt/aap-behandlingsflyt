package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.meldekort.DOM
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.verdityper.dokument.JournalpostId
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.aap.komponenter.type.Periode as DomenePeriode
import no.nav.aap.komponenter.verdityper.Prosent as DomeneProsent


data class RenderKontekst(
    val vedtak: List<BehandlingMedVedtak>,
    val overskriftsnivå: Int = 1,
) {
    fun forSubseksjon() = this.copy(overskriftsnivå = overskriftsnivå + 1)
}

data class Seksjon(
    val tittel: LøpendeTekst,
    val blokker: List<Blokk> = listOf(),
    val subseksjoner: List<Seksjon> = listOf(),
    /* Ikke støtte for frittstående avsnitt etter subseksjonene. Det blir ikke noe
     * visuelt skille mellom siste avsnitt i siste subseksjon, og et hypotetisk
     * etterfølgende avsnitt som ikke er en del av subseksjonen.
     */
) {
    constructor(tittel: String, vararg avsnitt: Blokk?) : this(
        tittel = Tekst(tittel),
        blokker = avsnitt.toList().filterNotNull(),
        subseksjoner = emptyList(),
    )

    constructor(tittel: LøpendeTekst, vararg avsnitt: Blokk?) : this(
        tittel = tittel,
        blokker = avsnitt.toList().filterNotNull(),
        subseksjoner = emptyList(),
    )

    constructor(tittel: String, vararg subseksjoner: Seksjon?) : this(
        tittel = Tekst(tittel),
        blokker = emptyList(),
        subseksjoner = subseksjoner.toList().filterNotNull(),
    )

    constructor(tittel: LøpendeTekst, vararg subseksjoner: Seksjon?) : this(
        tittel = tittel,
        blokker = emptyList(),
        subseksjoner = subseksjoner.toList().filterNotNull(),
    )

    fun render(kontekst: RenderKontekst): List<DOM> {
        return buildList {
            add(DOM.Header(kontekst.overskriftsnivå, tittel.render(kontekst)))

            for (blokk in blokker) {
                addAll(blokk.render(kontekst))
            }

            for (subseksjon in subseksjoner) {
                addAll(subseksjon.render(kontekst.forSubseksjon()))
            }
        }
    }
}

interface Blokk {
    fun render(kontekst: RenderKontekst): List<DOM>
}

data class Tabell(
    val kolonner: List<LøpendeTekst>,
    val rader: List<List<LøpendeTekst>>,
) : Blokk {
    override fun render(kontekst: RenderKontekst): List<DOM> = listOf(
        DOM.Tabell(
            kolonner = kolonner.map { it.render(kontekst) },
            rader = rader.map { it.map { it.render(kontekst) } },
        )
    )

    companion object {
        fun ofTidslinje(kolonner: List<LøpendeTekst>, tidslinje: Tidslinje<List<LøpendeTekst>>) = Tabell(
            kolonner = listOf(Tekst("Periode (fom – tom)")) + kolonner,
            rader = tidslinje.segmenter().map { (periode, rader) ->
                listOf(Periode(periode, kompakt = true)) + rader
            }
        )
    }
}

data class Avsnitt(
    val elementer: List<LøpendeTekst>,
) : Blokk {
    constructor(vararg elementer: LøpendeTekst?) : this(elementer.toList().filterNotNull())

    override fun render(kontekst: RenderKontekst) = listOf(
        DOM.Avsnitt(elementer.joinToString(separator = " ") { it.render(kontekst) })
    )
}

data class Fritekstfelt(val tittel: String?, val fritekst: String) : Blokk {
    override fun render(kontekst: RenderKontekst): List<DOM> = listOfNotNull(
        tittel?.let { DOM.Header(nivå = null, it) },
    ) + normaliserAvsnitt(fritekst)

    companion object {
        fun normaliserAvsnitt(fritekst: String): List<DOM.Avsnitt> {
            val linjer = fritekst.lines().map { it.trim() }
            var aktivtAvsnitt = mutableListOf<String>()
            val avsnitt = mutableListOf(aktivtAvsnitt)

            for (linje in linjer) {
                if (linje.isEmpty()) {
                    aktivtAvsnitt = mutableListOf()
                    avsnitt.addLast(aktivtAvsnitt)
                } else {
                    aktivtAvsnitt.addLast(linje)
                }
            }

            return avsnitt
                .filter { it.isNotEmpty() }
                .map { DOM.Avsnitt(it.joinToString(" ")) }
        }
    }
}

data class Dict(
    val valg: List<Pair<LøpendeTekst, LøpendeTekst>>,
) : Blokk {
    constructor(vararg valg: Pair<String, LøpendeTekst>) : this(
        valg.map { (key, value) -> Tekst(key) to value }
    )

    override fun render(kontekst: RenderKontekst) = listOf(
        DOM.List(
            valg.map { (key, value) ->
                key.render(kontekst) to value.render(kontekst)
            }
        )
    )
}

interface LøpendeTekst {
    fun render(kontekst: RenderKontekst): String
}

data class Span(val elementer: List<LøpendeTekst>) : LøpendeTekst {
    constructor(vararg elementer: LøpendeTekst?) : this(elementer.toList().filterNotNull())

    override fun render(kontekst: RenderKontekst) =
        elementer.joinToString(separator = "") { it.render(kontekst) }
}

fun Iterable<LøpendeTekst>.join(separator: String = ", "): LøpendeTekst {
    return Span(this.flatMap { listOf(Tekst(separator), it) }.drop(1))
}

fun <T> Iterable<T>.join(separator: String = ", ", mapper: (T) -> LøpendeTekst): LøpendeTekst {
    return Span(this.flatMap { listOf(Tekst(separator), mapper(it)) }.drop(1))
}

private val lineRegex = Regex("""[\n\r]""")

data class Tekst(val tekst: String) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) =
        tekst.replace(lineRegex, " ")
}

data class ReferanseBruker(val bruker: Bruker) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) =
        /** TODO: legg på navn / enhet fra kontekst */
        bruker.ident
}

data class ReferanseJournalpost(val journalpostId: JournalpostId) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) = "Journalpost ${journalpostId.identifikator}"
}

data class ReferanseBehandling(val behandlingId: BehandlingId) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst): String {
        val vedtakstidspunkt = kontekst.vedtak.singleOrNull { it.id == behandlingId }?.vedtakstidspunkt

        if (vedtakstidspunkt == null) {
            return behandlingId.toString()
        }

        /* Finn nødvendig oppløsning for å skille vedtak på samme dato. */
        val andreRelevanteVedtak = kontekst.vedtak
            .filter { it.id != behandlingId }
            .map { it.vedtakstidspunkt }
            .filter { it.toLocalDate() == vedtakstidspunkt.toLocalDate() }

        return buildString {
            append("vedtak fattet ")
            append(Dato(vedtakstidspunkt.toLocalDate()).render(kontekst))

            /* Mer enn ett vedtak på denne datoen, så legger med klokkeslett. */
            if (andreRelevanteVedtak.isNotEmpty()) {
                append(" ")
                append(vedtakstidspunkt.toLocalTime().toString())
            }
        }
    }
}

data class JaNeiValg(val valg: Boolean?) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) = when (valg) {
        true -> "ja"
        false -> "nei"
        null -> "ikke vurdert"
    }
}

data class PrettyEnum(val valg: Enum<*>?, val default: String = "—") : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) =
        valg?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "ikke vurdert"
}

data class Dato(val dato: LocalDate) : LøpendeTekst {
    constructor(tidspunkt: LocalDateTime) : this(tidspunkt.toLocalDate())

    override fun render(kontekst: RenderKontekst) =
        dato.format(formatter)

    companion object {
        val norwegian = Locale.of("no", "NO")
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy", norwegian)
        val kompaktFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", norwegian)
    }
}

data class Periode(val periode: DomenePeriode, val kompakt: Boolean = false) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) =
        if (kompakt)
            "${periode.fom.format(Dato.kompaktFormatter)} – ${periode.tom.format(Dato.kompaktFormatter)}"
        else
            "${periode.fom.format(Dato.formatter)} – ${periode.tom.format(Dato.formatter)}"
}

data class Tidspunkt(val tidspunkt: LocalDateTime) : LøpendeTekst {
    constructor(instant: Instant) : this(
        instant.atZone(ZoneId.of("europe/oslo")).toLocalDateTime()
    )

    override fun render(kontekst: RenderKontekst) =
        tidspunkt.toLocalDate().format(Dato.formatter) + " " + tidspunkt.toLocalTime().toString()
}

data class Prosent(val prosent: DomeneProsent) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) = "${prosent.prosentverdi()}%"
}

data class Kroner(val beløp: Beløp) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) = numberFormat.format(beløp.verdi())

    companion object {
        private val numberFormat = NumberFormat.getCurrencyInstance(Locale.of("no", "NO"))
    }
}

data class G(val gUnit: GUnit) : LøpendeTekst {
    override fun render(kontekst: RenderKontekst) = "${numberFormat.format(gUnit.verdi())} G"

    companion object {
        private val numberFormat = NumberFormat.getNumberInstance(Locale.of("no", "NO"))
    }
}

fun vurderingsoverskrift(
    behandling: BehandlingId,
    bruktForPeriode: DomenePeriode,
    hvem: Bruker,
): LøpendeTekst = Span(
    Tekst("Vurdering brukt for "),
    Periode(bruktForPeriode),
    Tekst(", vurdert av  "),
    ReferanseBruker(hvem),
    Tekst(" i "),
    ReferanseBehandling(behandling),
)

