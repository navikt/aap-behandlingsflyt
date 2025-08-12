package no.nav.aap.behandlingsflyt.behandling.klage.formkrav

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import java.time.LocalDate

data class FormkravGrunnlagDto(
    val vurdering: FormkravVurderingDto? = null,
    val varselSendtDato: LocalDate? = null,
    val varselSvarfrist: LocalDate? = null,
    val harTilgangTilÅSaksbehandle: Boolean
)

data class FormkravVurderingDto(
    val begrunnelse: String,
    val erBrukerPart: Boolean,
    val erFristOverholdt: Boolean,
    val likevelBehandles: Boolean?,
    val erKonkret: Boolean,
    val erSignert: Boolean,
    val vurdertAv: VurdertAvResponse?
)

internal fun FormkravVurdering.tilDto(ansattInfoService: AnsattInfoService) =
    FormkravVurderingDto(
        begrunnelse = begrunnelse,
        erBrukerPart = erBrukerPart,
        erFristOverholdt = erFristOverholdt,
        erKonkret = erKonkret,
        erSignert = erSignert,
        vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet, ansattInfoService),
        likevelBehandles = likevelBehandles
    )

internal fun FormkravGrunnlag.tilDto(
    harTilgangTilÅSaksbehandle: Boolean,
    ansattInfoService: AnsattInfoService
) =
    FormkravGrunnlagDto(
        vurdering = vurdering.tilDto(ansattInfoService),
        varselSendtDato = varsel?.sendtDato,
        varselSvarfrist = varsel?.svarfrist,
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
    )
