package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

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
    val vurdertAv: Bruker,
    val opprettetTid: Instant,
    val vurdertIBehandling: BehandlingId,
    val vurderingenGjelderFra: LocalDate,
    val vurderingenGjelderTil: LocalDate?
)

enum class EierVirksomhet{
    EIER_MINST_50_PROSENT,
    EIER_MINST_50_PROSENT_MED_FLER,
    NEI
}