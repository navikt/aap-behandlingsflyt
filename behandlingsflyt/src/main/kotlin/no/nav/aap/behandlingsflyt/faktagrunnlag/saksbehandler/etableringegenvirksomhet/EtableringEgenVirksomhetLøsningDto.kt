package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import kotlin.String

/**
 * @property fase Kan være null om vilkåret ikke er oppfylt.
 */
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

    val fase: EtableringFase? = null,
    val erRegistrertINødvendigeOffentligeRegister: Boolean? = null,
    val jobberBrukerAktivMedVirksomheten: Boolean? = null,

    //midlertid legacy, bare for overgang
    @Deprecated("Bruk fase + fom. Fjernes etter frontend mignering")
    val utviklingsPerioder: List<Periode>? = null,
    @Deprecated("Bruk fase + fom. Fjernes etter frontend mignering")
    val oppstartsPerioder: List<Periode>? = null
) : LøsningForPeriode {
    fun toEtableringEgenVirksomhetVurdering(avklaringsbehovKontekst: AvklaringsbehovKontekst) =
        toEtableringEgenVirksomhetVurdering(
            bruker = avklaringsbehovKontekst.bruker,
            vurdertIBehandling = avklaringsbehovKontekst.behandlingId(),
        )

    fun toEtableringEgenVirksomhetVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId): EtableringEgenVirksomhetVurdering {
        return EtableringEgenVirksomhetVurdering(
            begrunnelse = begrunnelse,
            foreliggerFagligVurdering = foreliggerFagligVurdering,
            virksomhetErNy = virksomhetErNy,
            brukerEierVirksomheten = brukerEierVirksomheten,
            kanFøreTilSelvforsørget = kanFøreTilSelvforsørget,
            fase = fase,
            utviklingsPerioder = if (fase == null) utviklingsPerioder.orEmpty() else emptyList(),
            oppstartsPerioder = if (fase == null) oppstartsPerioder.orEmpty() else emptyList(),
            erRegistrertINødvendigeOffentligeRegister = erRegistrertINødvendigeOffentligeRegister,
            jobberBrukerAktivMedVirksomheten = jobberBrukerAktivMedVirksomheten,
            vurdertAv = bruker,
            opprettet = Instant.now(),
            vurdertIBehandling = vurdertIBehandling,
            fom = fom,
            tom = tom,
            virksomhetNavn = virksomhetNavn,
            orgNr = orgNr,
            versjon = if (fase == null && (utviklingsPerioder != null || oppstartsPerioder != null))
                EtableringVersjon.V0 else EtableringVersjon.V1,
        )
    }

}
