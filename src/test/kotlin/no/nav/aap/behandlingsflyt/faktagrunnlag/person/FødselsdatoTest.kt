package no.nav.aap.behandlingsflyt.faktagrunnlag.person

import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FødselsdatoTest {

    @Test
    fun `brukers alder er riktig på en gitt dato`() {
        val fødselsdato = Fødselsdato(LocalDate.now().minusYears(25))
        val dagenIdag = LocalDate.now()

        assertThat(fødselsdato.alderPåDato(dagenIdag)).isEqualTo(25)
    }

    @Test
    fun `tester dagen før man har bursdag`() {
        val fødselsdato = Fødselsdato(LocalDate.now().minusYears(25))
        val dagenFør25årsdagen = LocalDate.now().minusDays(1)

        assertThat(fødselsdato.alderPåDato(dagenFør25årsdagen)).isEqualTo(24)
    }

    @Test
    fun `tester at man ikke kan sette fødselsdato inn i fremtiden`() {
        assertThrows<IllegalArgumentException> { Fødselsdato(LocalDate.now().plusDays(1)) }
    }

    @Test
    fun `fødselsdato er etter gitt dato så er alder negativ`() {
        val fødselsdato = Fødselsdato(LocalDate.now())
        val gittDato = LocalDate.now().minusYears(1)

        assertThat(fødselsdato.alderPåDato(gittDato)).isEqualTo(-1)
    }
}