package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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
    val erRegistrertINødvendigeOffentligeRegister: Boolean? = null,
    val jobberBrukerAktivMedVirksomheten: Boolean?,
    val fase: EtableringFase? = null,
    override val vurdertAv: Bruker,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate?
) : PeriodisertVurdering

enum class EierVirksomhet {
    EIER_MINST_50_PROSENT,
    EIER_MINST_50_PROSENT_MED_FLER,
    NEI
}

enum class EtableringFase {
    UTVIKLING,
    OPPSTART
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
        a.erRegistrertINødvendigeOffentligeRegister == b.erRegistrertINødvendigeOffentligeRegister &&
        a.jobberBrukerAktivMedVirksomheten == b.jobberBrukerAktivMedVirksomheten &&
        a.fase == b.fase &&
        a.fom == b.fom &&
        a.tom == b.tom
    }
}