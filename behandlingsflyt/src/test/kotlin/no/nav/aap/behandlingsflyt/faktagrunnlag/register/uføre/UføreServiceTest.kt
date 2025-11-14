package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UføreServiceTest {

    @Test
    fun `Ulik rekkefølge i lister skal ikke gi endring`() {
        val nå = LocalDate.now()
        val eksisterendeGrunnlag = UføreGrunnlag(
            BehandlingId(1), setOf(
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