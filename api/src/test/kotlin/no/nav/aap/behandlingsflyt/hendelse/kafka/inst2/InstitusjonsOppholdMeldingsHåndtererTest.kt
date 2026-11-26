package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsoppholdKilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsoppholdsType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class InstitusjonsOppholdMeldingsHåndtererTest {
    @Test
    fun `videresender meldingen med partisjon og offset som nøkkel uten Kafka`() {
        val mottatte = mutableListOf<Pair<String, InstitusjonsOppholdHendelseKafkaMelding>>()
        val håndterer = InstitusjonsOppholdMeldingsHåndterer { nøkkel, melding ->
            mottatte.add(nøkkel to melding)
        }
        val melding = melding()

        håndterer.håndter(ConsumerRecord("institusjonsopphold", 2, 42L, "kafka-nøkkel", melding))

        assertThat(mottatte).containsExactly("2-42" to melding)
    }

    @Test
    fun `ignorerer meldinger uten kilde`() {
        val mottatte = mutableListOf<Pair<String, InstitusjonsOppholdHendelseKafkaMelding>>()
        val håndterer = InstitusjonsOppholdMeldingsHåndterer { nøkkel, melding ->
            mottatte.add(nøkkel to melding)
        }
        håndterer.håndter(ConsumerRecord("institusjonsopphold", 2, 41L, "uten-kilde", melding(kilde = null)))

        assertThat(mottatte).isEmpty()
    }

    @Test
    fun `lar feil fra hendelseshåndteringen propagere til konsumenten`() {
        val feil = IllegalStateException("Kunne ikke lagre hendelsen")
        val mottatteNøkler = mutableListOf<String>()
        val håndterer = InstitusjonsOppholdMeldingsHåndterer { nøkkel, _ ->
            mottatteNøkler.add(nøkkel)
            throw feil
        }

        assertThatThrownBy {
            håndterer.håndter(ConsumerRecord("institusjonsopphold", 2, 41L, "første", melding()))
        }.isSameAs(feil)
        assertThat(mottatteNøkler).containsExactly("2-41")
    }

    private fun melding(kilde: InstitusjonsoppholdKilde? = InstitusjonsoppholdKilde.INST) =
        InstitusjonsOppholdHendelseKafkaMelding(
            hendelseId = 1,
            oppholdId = 1,
            norskident = "12345678901",
            type = InstitusjonsoppholdsType.OPPDATERING,
            kilde = kilde,
            institusjonsOpphold = null
        )
}
