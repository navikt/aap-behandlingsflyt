package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Prosent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

class UføreServiceTest {
    
    @Test
    fun `Ulik rekkefølge i lister skal ikke gi endring`() {
        val nå = LocalDate.now()
        val eksisterendeGrunnlag = UføreGrunnlag(
            1, BehandlingId(1), listOf(
                Uføre(nå, Prosent.`50_PROSENT`),
                Uføre(nå.plusDays(2), Prosent.`30_PROSENT`),
            )
        )

        val ny = listOf(
            Uføre(nå.plusDays(2), Prosent.`30_PROSENT`),
            Uføre(nå, Prosent.`50_PROSENT`),
        )
        
        assertFalse(UføreService.harEndringerUføre(eksisterendeGrunnlag, ny))

    }

}