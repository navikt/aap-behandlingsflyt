package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.dokumentasjon.Blokker
import no.nav.aap.behandlingsflyt.dokumentasjon.Dict
import no.nav.aap.behandlingsflyt.dokumentasjon.Div
import no.nav.aap.behandlingsflyt.dokumentasjon.Fritekstfelt
import no.nav.aap.behandlingsflyt.dokumentasjon.JaNeiValg
import no.nav.aap.behandlingsflyt.dokumentasjon.PrettyEnum
import no.nav.aap.behandlingsflyt.dokumentasjon.Tabell
import no.nav.aap.behandlingsflyt.dokumentasjon.Tekst
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class EtableringEgenVirksomhetVurdering(
    val begrunnelse: String,
    val virksomhetNavn: String,
    val orgNr: String? = null,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean?,
    val brukerEierVirksomheten: EierVirksomhet?,
    val kanFøreTilSelvforsørget: Boolean?,
    val utviklingsPerioder: List<Periode>,
    val oppstartsPerioder: List<Periode>,
    override val vurdertAv: Bruker,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate?
) : PeriodisertVurdering {
    override fun genererDokumentasjon(): Blokker {
        return Div(
            Fritekstfelt("Begrunnelse", begrunnelse),
            Dict(
                "Virksomhetsnavn" to Tekst(virksomhetNavn),
                "Org.nr." to Tekst(orgNr ?: "—"),
                "Foreligger det en næringsfaglig vurdering?" to JaNeiValg(foreliggerFagligVurdering),
                "Er virksomheten ny?" to JaNeiValg(virksomhetErNy),
                "Eier bruker virksomheten?" to PrettyEnum(brukerEierVirksomheten),
                "Antas det at etablering av virksomheten vil føre til at bruker blir selvforsørget?" to JaNeiValg(kanFøreTilSelvforsørget),
            ),
            Tabell(
                kolonner = listOf(Tekst("Utviklingsfase")),
                rader = utviklingsPerioder.map {
                    listOf(no.nav.aap.behandlingsflyt.dokumentasjon.Periode(it))
                }
            ),
            Tabell(
                kolonner = listOf(Tekst("Oppstartsfase")),
                rader = oppstartsPerioder.map {
                    listOf(no.nav.aap.behandlingsflyt.dokumentasjon.Periode(it))
                }
            ),
        )
    }
}

enum class EierVirksomhet {
    EIER_MINST_50_PROSENT,
    EIER_MINST_50_PROSENT_MED_FLER,
    NEI
}

fun List<EtableringEgenVirksomhetVurdering>.erFunksjoneltLik(other: List<EtableringEgenVirksomhetVurdering>): Boolean {
    if (this.size != other.size) return false

    return this.zip(other).all { (a, b) ->
        a.begrunnelse == b.begrunnelse &&
                a.virksomhetNavn == b.virksomhetNavn &&
                a.orgNr == b.orgNr &&
                a.foreliggerFagligVurdering == b.foreliggerFagligVurdering &&
                a.virksomhetErNy == b.virksomhetErNy &&
                a.brukerEierVirksomheten == b.brukerEierVirksomheten &&
                a.kanFøreTilSelvforsørget == b.kanFøreTilSelvforsørget &&
                a.utviklingsPerioder == b.utviklingsPerioder &&
                a.oppstartsPerioder == b.oppstartsPerioder &&
                a.fom == b.fom &&
                a.tom == b.tom
    }
}