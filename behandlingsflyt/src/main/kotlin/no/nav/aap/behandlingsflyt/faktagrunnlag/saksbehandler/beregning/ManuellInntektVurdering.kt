package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDateTime
import java.time.Year

data class ManuellInntektVurdering(
    val år: Year,
    val begrunnelse: String,
    val belop: Beløp? = null,
    val vurdertAv: String,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val eosBelop: Beløp? = null,
)