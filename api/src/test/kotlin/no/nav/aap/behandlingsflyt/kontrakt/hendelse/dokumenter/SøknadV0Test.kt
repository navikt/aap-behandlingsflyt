package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SøknadV0Test {

    @Test
    fun `SøknadStudentDto serde`() {
        val dto = SøknadStudentDto(
            erStudent = StudentStatus.Ja,
            kommeTilbake = KommeTilbake.VetIkke
        )

        val json = DefaultJsonMapper.toJson(dto)

        val deserialized = DefaultJsonMapper.fromJson(json, SøknadStudentDto::class.java)

        assertThat(deserialized).isEqualTo(dto)
    }

    @ParameterizedTest
    @EnumSource(KommeTilbake::class)
    fun `KommeTilbake serde`(kommeTilbake: KommeTilbake) {
        val json = DefaultJsonMapper.toJson(kommeTilbake)

        assertThat(json).isEqualTo("\"${kommeTilbake.customValue()}\"")

        val deserialized = DefaultJsonMapper.fromJson(json, KommeTilbake::class.java)

        assertThat(deserialized).isEqualTo(kommeTilbake)

        assertThat(DefaultJsonMapper.fromJson("\"${kommeTilbake.name}\"", KommeTilbake::class.java)).isEqualTo(kommeTilbake)
    }

}
