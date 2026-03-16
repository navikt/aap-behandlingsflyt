package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import java.time.LocalDate

data class VedtakslengdeVurderingDto(
    val sluttdato: LocalDate,
    val begrunnelse: String,
)

