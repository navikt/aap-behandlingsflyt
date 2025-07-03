package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

class DefinisjonTest {

    @Test
    fun `Skal validere OK for alle definisjoner`() {
        try {
            Definisjon.entries.toTypedArray()
        } catch (e: Exception) {
            fail(e)
        }
    }

    @Test
    fun `serialisere og deserialisere fungerer`() {
        Definisjon.entries.forEach {
            val json = DefaultJsonMapper.toJson(it)
            val tilbake = DefaultJsonMapper.fromJson<Definisjon>(json)
            assertThat(tilbake).isEqualTo(it)
        }
    }

    @Test
    fun `Skal ikke endre eller slette eksisterende definisjoner`() {
        val mapper = ObjectMapper()
        
        val snapshot = File("definisjoner_snapshot.json").readText()
        
        val rotnode = mapper.readTree(snapshot)
        val snapshotListe = rotnode.map { mapper.writeValueAsString(it) }

        val definisjonerListe: List<String> = Definisjon.entries.map { mapper.writeValueAsString(it) }

        assertThat(definisjonerListe).containsAll(snapshotListe)

        File("definisjoner_snapshot.json")
            .writeText(DefaultJsonMapper.toJson(Definisjon.entries))
    }
}
