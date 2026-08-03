package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate


data class SykepengerErstatningGrunnlag(
    val vurderinger: List<SykepengerVurdering>
) {
    fun somTidslinje(kravDato: LocalDate, sisteMuligDagMedYtelse: LocalDate): Tidslinje<SykepengerVurdering> {
        return vurderinger
            .gjeldendeVurderinger()
            .begrensetTil(Periode(fom = kravDato, tom = sisteMuligDagMedYtelse))
    }
}
