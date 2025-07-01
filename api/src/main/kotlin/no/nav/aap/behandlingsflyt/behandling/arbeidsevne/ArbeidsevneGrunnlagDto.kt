package no.nav.aap.behandlingsflyt.behandling.arbeidsevne

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattNavnOgEnhet
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate.FastsettArbeidsevneDto
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevneGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurderinger: List<ArbeidsevneVurderingDto>?,
    val gjeldendeVedtatteVurderinger: List<ArbeidsevneVurderingDto>?,
    val historikk: Set<ArbeidsevneVurderingDto>?
)

data class ArbeidsevneVurderingDto(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val arbeidsevne: Int,
    val fraDato: LocalDate,
    val vurdertAv: VurdertAvResponse
)

fun ArbeidsevneVurdering.toDto(ansattNavnOgEnhet: AnsattNavnOgEnhet? = null): ArbeidsevneVurderingDto =
    ArbeidsevneVurderingDto(
        begrunnelse,
        opprettetTid ?: LocalDateTime.now(),
        arbeidsevne.prosentverdi(),
        fraDato,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = requireNotNull(opprettetTid?.toLocalDate()) { "Fant ikke vurdert tidspunkt for arbeidsevnevurdering" },
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        )
    )

data class SimulerArbeidsevneDto(
    val vurderinger: List<SimuleringArbeidsevneVurderingDto>
)

data class SimuleringArbeidsevneVurderingDto(
    val begrunnelse: String,
    val arbeidsevne: Int,
    val fraDato: LocalDate
)

data class SimulertArbeidsevneResultatDto(
    val gjeldendeVedtatteVurderinger: List<ArbeidsevneVurderingDto>
)