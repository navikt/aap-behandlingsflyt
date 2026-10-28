package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.somLegacyVisning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class EtableringEgenVirksomhetGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<EtableringEgenVirksomhetVurderingResponse>,
    override val nyeVurderinger: List<EtableringEgenVirksomhetVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    override val ikkeRelevantePerioder: List<Periode>,
    val vurderingerMeta: VurderingerMetaResponse,
    val bruktUtviklingsDager: Int?,
    val bruktOppstartsdager: Int?,
    val tom: LocalDate?
) : PeriodiserteVurderingerDto<EtableringEgenVirksomhetVurderingResponse>

data class EtableringEgenVirksomhetVurderingResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurderingerMeta: VurderingerMetaResponse,

    val begrunnelse: String,
    val virksomhetNavn: String,
    val orgNr: String?,
    val foreliggerFagligVurdering: Boolean,
    val virksomhetErNy: Boolean?,
    val brukerEierVirksomheten: EierVirksomhet?,
    val kanFøreTilSelvforsørget: Boolean?,
    val utviklingsPeriode: List<Periode>?,
    val oppstartsPeriode: List<Periode>?,
    val oppfylt: Boolean,
    val fase: EtableringFase?,
    val erRegistrertINødvendigeOffentligeRegister: Boolean?,
    val jobberBrukerAktivMedVirksomheten: Boolean?,
) : VurderingDto {
    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<EtableringEgenVirksomhetVurdering>,
            vurdertAvService: VurdertAvService,
        ): List<EtableringEgenVirksomhetVurderingResponse> {
            return tidslinje.segmenter()
                .map { segment ->
                    fraDomene(
                        etableringEgenVirksomhetVurdering = segment.verdi,
                        vurdertAvService = vurdertAvService,
                        fom = segment.fom(),
                        tom = segment.tom().takeUnless { it == Tid.MAKS && segment.verdi.tom == null },
                    )
                }
        }

        fun fraDomene(
            etableringEgenVirksomhetVurdering: EtableringEgenVirksomhetVurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = etableringEgenVirksomhetVurdering.fom,
            tom: LocalDate? = etableringEgenVirksomhetVurdering.tom
        ): EtableringEgenVirksomhetVurderingResponse {
            val legacyVisning = etableringEgenVirksomhetVurdering.copy(fom = fom, tom = tom).somLegacyVisning()
            return EtableringEgenVirksomhetVurderingResponse(
                begrunnelse = etableringEgenVirksomhetVurdering.begrunnelse,
                virksomhetNavn = etableringEgenVirksomhetVurdering.virksomhetNavn,
                orgNr = etableringEgenVirksomhetVurdering.orgNr,
                foreliggerFagligVurdering = etableringEgenVirksomhetVurdering.foreliggerFagligVurdering,
                virksomhetErNy = etableringEgenVirksomhetVurdering.virksomhetErNy,
                brukerEierVirksomheten = etableringEgenVirksomhetVurdering.brukerEierVirksomheten,
                kanFøreTilSelvforsørget = etableringEgenVirksomhetVurdering.kanFøreTilSelvforsørget,
                utviklingsPeriode = legacyVisning.utviklingsPerioder,
                oppstartsPeriode = legacyVisning.oppstartsPerioder,
                vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                    definisjon = Definisjon.ETABLERING_EGEN_VIRKSOMHET,
                    behandlingId = etableringEgenVirksomhetVurdering.vurdertIBehandling,
                    vurdertAv = vurdertAvService.medNavnOgEnhet(
                        etableringEgenVirksomhetVurdering.vurdertAv,
                        etableringEgenVirksomhetVurdering.opprettet,
                    ),
                ),
                fom = fom,
                tom = tom,
                oppfylt = etableringEgenVirksomhetVurdering.evaluerVirksomhetVurdering(),
                fase = etableringEgenVirksomhetVurdering.fase,
                erRegistrertINødvendigeOffentligeRegister = etableringEgenVirksomhetVurdering.erRegistrertINødvendigeOffentligeRegister,
                jobberBrukerAktivMedVirksomheten = etableringEgenVirksomhetVurdering.jobberBrukerAktivMedVirksomheten
            )
        }
    }
}
