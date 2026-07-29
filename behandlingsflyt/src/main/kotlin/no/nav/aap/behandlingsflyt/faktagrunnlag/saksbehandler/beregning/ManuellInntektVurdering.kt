package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime
import java.time.Year

data class ManuellInntektVurdering(
    val år: Year,
    val begrunnelse: String,
    val belop: Beløp? = null,
    val vurdertAv: Bruker,
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val eøsBeløp: Beløp? = null,
    val ferdigLignetPGI: Beløp? = null,
    /**
     * Delperiode innen [år] når inntekten gjelder en del av året (f.eks. før/etter endring i
     * uføregrad).
     */
    val månedsPeriode: Periode? = null,
) {
    init {
        require(månedsPeriode == null || Year.from(månedsPeriode.fom) == år && Year.from(månedsPeriode.tom) == år) {
            "Delperiode $månedsPeriode må ligge innenfor året $år"
        }
    }
}