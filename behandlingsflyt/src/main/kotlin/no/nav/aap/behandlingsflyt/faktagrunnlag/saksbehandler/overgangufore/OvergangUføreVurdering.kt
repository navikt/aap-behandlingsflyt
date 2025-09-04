package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import java.time.Instant
import java.time.LocalDate

data class OvergangUføreVurdering(
    val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: String?,
    val brukerRettPåAAP: Boolean?,
    val virkningsdato: LocalDate?,
    val vurdertAv: String,
    val vurderingenGjelderFra: LocalDate?,
    val opprettet: Instant? = null
)

