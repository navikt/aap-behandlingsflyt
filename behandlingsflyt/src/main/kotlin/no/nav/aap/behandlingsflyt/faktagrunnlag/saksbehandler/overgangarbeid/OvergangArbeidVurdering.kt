package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid

import java.time.Instant
import java.time.LocalDate

data class OvergangArbeidVurdering(
    val begrunnelse: String,
    val brukerRettPåAAP: Boolean?,
    val virkningsdato: LocalDate?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate?,
    val opprettet: Instant? = null
)