package no.nav.aap.sykepengererstatning

import java.time.LocalDate
import no.nav.aap.misc.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode

data class SykepengerErstatningGrunnlag(
    val vurderinger: List<SykepengerVurdering>
) {
    fun somTidslinje(kravDato: LocalDate, sisteMuligDagMedYtelse: LocalDate): Tidslinje<SykepengerVurdering> {
        return vurderinger
            .gjeldendeVurderinger()
            .begrensetTil(Periode(fom = kravDato, tom = sisteMuligDagMedYtelse))
    }
}