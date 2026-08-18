package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.Test

class MottattHendelseServiceThrottlingTest {

    @Test
    fun `Skal gi deterministisk neste_kjoring for samme referanse`() {
        val referanse = InnsendingReferanse(JournalpostId("123456789"))

        val førsteBeregning = beregnNesteKjøringForMeldekort(referanse)
        val andreBeregning = beregnNesteKjøringForMeldekort(referanse)

        // Samme referanse skal gi (tilnærmet) samme forsinkelse i sekunder, uavhengig av
        // hvilket LocalDateTime.now() de to kallene ble evaluert mot. Vi sjekker derfor at
        // begge havner innenfor 1 sekund av hverandre, ikke eksakt likt tidspunkt.
        assertThat(Duration.between(førsteBeregning, andreBeregning).abs())
            .isLessThan(Duration.ofSeconds(1))
    }

    @Test
    fun `Skal spre neste_kjoring for ulike referanser utover strupevinduet`() {
        val referanser = (1..50).map { InnsendingReferanse(JournalpostId(it.toString())) }
        val tidspunktFør = LocalDateTime.now()

        val nesteKjøringer = referanser.map { beregnNesteKjøringForMeldekort(it) }

        // Alle beregnede tidspunkt skal ligge i fremtiden, innenfor strupevinduet
        assertThat(nesteKjøringer).allSatisfy { tidspunkt ->
            assertThat(tidspunkt).isAfterOrEqualTo(tidspunktFør)
            assertThat(tidspunkt).isBefore(tidspunktFør.plusSeconds(MELDEKORT_STRUP_VINDU_SEKUNDER + 1))
        }

        // Med 50 ulike referanser forventer vi at de spres til flere enn ett unikt tidspunkt,
        // altså at det faktisk er jitter og ikke en konstant forsinkelse.
        assertThat(nesteKjøringer.toSet().size).isGreaterThan(1)
    }
}
