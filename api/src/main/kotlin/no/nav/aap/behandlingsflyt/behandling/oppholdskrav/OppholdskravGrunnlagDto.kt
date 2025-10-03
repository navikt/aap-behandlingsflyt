package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import java.time.LocalDate

data class OppholdskravGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val oppholdskravVurdering: OppholdskravVurderingDto?,
    val gjeldendeVedtatteVurderinger: List<TidligereOppholdskravVurderingDto>
)

data class OppholdskravVurderingDto(
    val vurdertAv: VurdertAvResponse? = null,
    val perioder: List<OppholdskravPeriodeDto>,
)

data class OppholdskravPeriodeDto(
    val oppfylt: Boolean,
    val begrunnelse: String,
    val land: String?,
    val fom: LocalDate,
    val tom: LocalDate? = null,
)

data class TidligereOppholdskravVurderingDto(
    val vurdertAv: VurdertAvResponse? = null,
    val oppfylt: Boolean,
    val begrunnelse: String,
    val land: String?,
    val fom: LocalDate,
    val tom: LocalDate? = null,
)

fun OppholdskravVurdering.tilDto(ansattInfoService: AnsattInfoService): OppholdskravVurderingDto =
    OppholdskravVurderingDto(
        perioder = perioder.map {
            OppholdskravPeriodeDto(
                oppfylt = it.oppfylt,
                begrunnelse = it.begrunnelse,
                land = it.land,
                fom = it.fom,
                tom = it.tom,
            )
        },
        vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet.toLocalDate(), ansattInfoService)
    )