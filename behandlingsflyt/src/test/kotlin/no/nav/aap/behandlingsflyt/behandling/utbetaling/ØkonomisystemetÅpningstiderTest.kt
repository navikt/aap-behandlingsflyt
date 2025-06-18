package no.nav.aap.behandlingsflyt.behandling.utbetaling

import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class ØkonomisystemetÅpningstiderTest {

    @Test
    fun `Skal være åpen på en vanlig virkedag`() {
        val status = ØkonomisystemetÅpningstider.sjekk(LocalDate.of(2025, 6, 18).atTime(11,27))
        assertThat(status).isInstanceOf(Åpent::class.java)
    }

    @Test
    fun `Skal være stengt før kl 6 på en vanlig virkedag`() {
        val status = ØkonomisystemetÅpningstider.sjekk(LocalDate.of(2025, 6, 18).atTime(5,30))
        assertThat(status).isInstanceOf(Stengt::class.java)
        assertThat((status as Stengt).åpner).isEqualTo(LocalDate.of(2025, 6, 18).atTime(6,0))
    }

    @Test
    fun `Skal være stengt etter kl 9 på en vanlig virkedag`() {
        val status = ØkonomisystemetÅpningstider.sjekk(LocalDate.of(2025, 6, 18).atTime(21,45))
        assertThat(status).isInstanceOf(Stengt::class.java)
        assertThat((status as Stengt).åpner).isEqualTo(LocalDate.of(2025, 6, 19).atTime(6,0))
    }

    @Test
    fun `Skal være stengt på lørdag og åpner på mandag kl 6`() {
        val status = ØkonomisystemetÅpningstider.sjekk(LocalDate.of(2025, 6, 14).atTime(9,45))
        assertThat(status).isInstanceOf(Stengt::class.java)
        assertThat((status as Stengt).åpner).isEqualTo(LocalDate.of(2025, 6, 16).atTime(6,0))
    }

    @Test
    fun `Skal være stengt på søndag og åpner på mandag kl 6`() {
        val status = ØkonomisystemetÅpningstider.sjekk(LocalDate.of(2025, 6, 15).atTime(19,20))
        assertThat(status).isInstanceOf(Stengt::class.java)
        assertThat((status as Stengt).åpner).isEqualTo(LocalDate.of(2025, 6, 16).atTime(6,0))
    }

    @Test
    fun `Skal være stengt første mai og åpner neste dag kl 6`() {
        val status = ØkonomisystemetÅpningstider.sjekk(LocalDate.of(2025, 5, 1).atTime(12,20))
        assertThat(status).isInstanceOf(Stengt::class.java)
        assertThat((status as Stengt).åpner).isEqualTo(LocalDate.of(2025, 5, 2).atTime(6,0))
    }

}