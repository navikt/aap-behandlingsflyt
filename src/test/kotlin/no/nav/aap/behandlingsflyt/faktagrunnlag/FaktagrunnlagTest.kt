package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskadedata
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskadedatalager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FaktagrunnlagTest {

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        val faktagrunnlag = Faktagrunnlag()

        faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(Yrkesskade))
        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(Yrkesskade))

        assertThat(erOppdatert).isEmpty()
    }

    @Test
    fun `Yrkesskadedata er ikke oppdatert`() {
        val faktagrunnlag = Faktagrunnlag()

        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(Yrkesskade))

        assertThat(erOppdatert)
            .hasSize(1)
            .allMatch { it === Yrkesskade }
    }

    @Test
    fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
        val yrkesskadedatalager = Yrkesskadedatalager()
        val faktagrunnlag = Faktagrunnlag(listOf(Yrkesskade(datalager = yrkesskadedatalager)))

        yrkesskadedatalager.lagre(Yrkesskadedata(), LocalDateTime.now().minusDays(1))

        val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(Yrkesskade))

        assertThat(erOppdatert).isEmpty()
    }
}
