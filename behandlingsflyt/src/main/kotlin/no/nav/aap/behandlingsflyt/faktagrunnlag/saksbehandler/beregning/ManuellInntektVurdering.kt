package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDateTime
import java.time.Year

data class ManuellInntektVurdering(
    val år: Year,
    val begrunnelse: String,
    val belop: Beløp? = null,
    val vurdertAv: String,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val eøsBeløp: Beløp? = null,
    val ferdigLignetPGI: Beløp? = null,
    /**
     * Delperiode innen [år] når inntekten gjelder en del av året (f.eks. før/etter endring i
     * uføregrad).
     */
    val periode: Periode? = null,
) {
    init {
        require(periode == null || Year.from(periode.fom) == år && Year.from(periode.tom) == år) {
            "Delperiode $periode må ligge innenfor året $år"
        }
    }
}