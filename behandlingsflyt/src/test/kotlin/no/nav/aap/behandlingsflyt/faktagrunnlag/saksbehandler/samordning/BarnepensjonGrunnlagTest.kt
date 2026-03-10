package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonPeriode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.test.Test

class BarnepensjonGrunnlagTest {
    @Test
    fun `Månedsats er dagsats ganger 12 delt på 260 (hverdager) rundet av til nærmeste hele krone`() {
            val periode = BarnepensjonPeriode(
                fom = YearMonth.of(2020, 1),
                tom = YearMonth.of(2020, 12),
                månedsats = Beløp(10000)
            )
        
        assertThat(periode.dagsats().verdi()).isEqualByComparingTo(BigDecimal(462))
    }
}