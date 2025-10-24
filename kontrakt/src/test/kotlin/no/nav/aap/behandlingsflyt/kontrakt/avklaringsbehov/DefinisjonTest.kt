package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class DefinisjonTest {
    @Test
    fun `definisjoner har unik kode, ingen duplikater`() {
        val kodeMap = Definisjon.entries.associateBy { it.kode }
        assertThat(kodeMap.keys).doesNotHaveDuplicates()
        assertThat(kodeMap).hasSize(Definisjon.entries.size)
    }

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
}
