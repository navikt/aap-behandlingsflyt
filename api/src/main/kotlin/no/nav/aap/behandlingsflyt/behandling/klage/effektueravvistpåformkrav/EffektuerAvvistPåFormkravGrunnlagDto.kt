package no.nav.aap.behandlingsflyt.behandling.klage.effektueravvistpåformkrav

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.ForhåndsvarselKlageFormkrav
import java.time.LocalDate

data class EffektuerAvvistPåFormkravGrunnlagDto(
    val varsel: EffektuerAvvistPåFormkravVarselDto? = null,
    val vurdering: EffektuerAvvistPåFormkravVurderingDto? = null,
    val harTilgangTilÅSaksbehandle: Boolean
)

internal fun EffektuerAvvistPåFormkravGrunnlag.tilDto(brevFerdigstilt: LocalDate?, harTilgangTilÅSaksbehandle: Boolean): EffektuerAvvistPåFormkravGrunnlagDto {
    return EffektuerAvvistPåFormkravGrunnlagDto(
        varsel = this.varsel.tilDto(brevFerdigstilt),
        vurdering = this.vurdering?.tilDto(),
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
    )
}

data class EffektuerAvvistPåFormkravVarselDto(
    val frist: LocalDate?,
    val brevFerdigstilt: LocalDate?,
)

internal fun ForhåndsvarselKlageFormkrav.tilDto(brevFerdigstilt: LocalDate?): EffektuerAvvistPåFormkravVarselDto {
    return EffektuerAvvistPåFormkravVarselDto(
        brevFerdigstilt = brevFerdigstilt,
        frist = this.frist
    )
}

data class EffektuerAvvistPåFormkravVurderingDto(
    val skalEndeligAvvises: Boolean
)

internal fun EffektuerAvvistPåFormkravVurdering.tilDto(): EffektuerAvvistPåFormkravVurderingDto {
    return EffektuerAvvistPåFormkravVurderingDto(
        skalEndeligAvvises = this.skalEndeligAvvises
    )
}