package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import java.time.Instant
import java.time.LocalDate

data class OvergangUføreVurdering(
    val begrunnelse: String,
    val brukerSoktUforetrygd: Boolean,
    val brukerVedtakUforetrygd: String?,
    val brukerRettPaaAAP: Boolean?,
    val virkningsDato: LocalDate?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate?,
    val opprettet: Instant? = null
)

