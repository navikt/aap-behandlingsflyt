package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import kotlin.String

data class EtableringEgenVirksomhetLøsningDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val virksomhetNavn: String,
    val orgNr: String? = null,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean? = null,
    val brukerEierVirksomheten: EierVirksomhet? = null,
    val kanFøreTilSelvforsørget: Boolean? = null,
    val utviklingsPerioder: List<Periode>,
    val oppstartsPerioder: List<Periode>
) : LøsningForPeriode {
    fun toEtableringEgenVirksomhetVurdering(avklaringsbehovKontekst: AvklaringsbehovKontekst) =
        toEtableringEgenVirksomhetVurdering(
            bruker = avklaringsbehovKontekst.bruker,
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
        )

    fun toEtableringEgenVirksomhetVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId) =
        EtableringEgenVirksomhetVurdering(
            begrunnelse = begrunnelse,
            foreliggerFagligVurdering = foreliggerFagligVurdering,
            virksomhetErNy = virksomhetErNy,
            brukerEierVirksomheten = brukerEierVirksomheten,
            kanFøreTilSelvforsørget = kanFøreTilSelvforsørget,
            utviklingsPerioder = utviklingsPerioder,
            oppstartsPerioder = oppstartsPerioder,
            vurdertAv = bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = vurdertIBehandling,
            fom = fom,
            tom = tom,
            virksomhetNavn = virksomhetNavn,
            orgNr = orgNr
        )
}