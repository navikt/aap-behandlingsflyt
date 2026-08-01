package no.nav.aap.etableringegenvirksomhet

import java.time.Instant
import java.time.LocalDate
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.misc.PeriodisertVurdering

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
    val vurdertAv: Bruker,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate?
) : PeriodisertVurdering

enum class EierVirksomhet{
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