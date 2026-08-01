package no.nav.aap.beregning

import java.time.LocalDateTime
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker

/**
 * Referanse svarer til saksnummeret til yrkesskaden fra registeret.
 */
data class YrkesskadeBeløpVurdering(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime? = null
)