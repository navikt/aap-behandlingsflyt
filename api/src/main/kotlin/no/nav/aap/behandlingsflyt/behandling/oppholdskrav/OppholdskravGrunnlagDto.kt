package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class OppholdskravGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<OppholdskravVurderingDto>,
    override val nyeVurderinger: List<OppholdskravVurderingDto>,
    override val kanVurderes: List<Periode>,
    override val ikkeRelevantePerioder: List<Periode>,
    override val behøverVurderinger: List<Periode>
) : PeriodiserteVurderingerDto<OppholdskravVurderingDto>

data class OppholdskravVurderingDto(
    val oppfylt: Boolean,
    val begrunnelse: String,
    val land: String?,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurderingerMeta: VurderingerMetaResponse,
): VurderingDto

fun OppholdskravVurdering.tilDto(
    ansattInfoService: AnsattInfoService,
    vurdertAvService: VurdertAvService
): List<OppholdskravVurderingDto> =
    perioder.map {
        OppholdskravVurderingDto(
            vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                definisjon = Definisjon.AVKLAR_OPPHOLDSKRAV,
                behandlingId = vurdertIBehandling,
                vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet.toLocalDate(), ansattInfoService),
            ),
            oppfylt = it.oppfylt,
            begrunnelse = it.begrunnelse,
            land = it.land,
            fom = it.fom,
            tom = it.tom,
        )
    }
