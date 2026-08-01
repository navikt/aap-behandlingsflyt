package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.beregning.Uføre
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.beregning.UføreGrunnlag

class UføreServiceTest {

    @Test
    fun `Ulik rekkefølge i lister skal ikke gi endring`() {
        val nå = LocalDate.now()
        val eksisterendeGrunnlag = UføreGrunnlag(
            setOf(
                Uføre(nå, Prosent.`50_PROSENT`),
                Uføre(nå.plusDays(2), Prosent.`30_PROSENT`),
            )
        )

        val ny = setOf(
            Uføre(nå.plusDays(2), Prosent.`30_PROSENT`),
            Uføre(nå, Prosent.`50_PROSENT`),
        )

        assertThat(UføreInformasjonskrav.harEndringerUføre(eksisterendeGrunnlag, ny)).isFalse
    }

}