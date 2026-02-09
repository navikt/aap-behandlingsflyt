package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.komponenter.type.Periode
import java.time.Instant
import java.time.LocalDate
import kotlin.String

data class EtableringEgenVirksomhetLøsningDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val virksomhetNavn: String,
    val orgNr: Long? = null,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean,
    val brukerEierVirksomheten: Boolean,
    val kanFøreTilSelvforsørget: Boolean,
    val utviklingsPerioder: List<Periode>,
    val oppstartsPerioder: List<Periode>
) : LøsningForPeriode {
    fun toEtableringEgenVirksomhetVurdering(avklaringsbehovKontekst: AvklaringsbehovKontekst) =
        EtableringEgenVirksomhetVurdering(
            begrunnelse = begrunnelse,
            foreliggerFagligVurdering = foreliggerFagligVurdering,
            virksomhetErNy = virksomhetErNy,
            brukerEierVirksomheten = brukerEierVirksomheten,
            kanFøreTilSelvforsørget = kanFøreTilSelvforsørget,
            utviklingsPerioder = utviklingsPerioder,
            oppstartsPerioder = oppstartsPerioder,
            vurdertAv = avklaringsbehovKontekst.bruker,
            opprettetTid = Instant.now(),
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
            vurderingenGjelderFra = fom,
            vurderingenGjelderTil = tom,
            virksomhetNavn = virksomhetNavn,
            orgNr = orgNr
        )
}