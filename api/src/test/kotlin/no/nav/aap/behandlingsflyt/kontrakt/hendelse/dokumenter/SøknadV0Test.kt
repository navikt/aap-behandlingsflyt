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
            kommeTilbake = JaNeiVetIkke.VetIkke
        )

        val json = DefaultJsonMapper.toJson(dto)

        val deserialized = DefaultJsonMapper.fromJson(json, SøknadStudentDto::class.java)

        assertThat(deserialized).isEqualTo(dto)
    }

    @ParameterizedTest
    @EnumSource(JaNeiVetIkke::class)
    fun `KommeTilbake serde`(jaNeiVetIkke: JaNeiVetIkke) {
        val json = DefaultJsonMapper.toJson(jaNeiVetIkke)

        assertThat(json).isEqualTo("\"${jaNeiVetIkke.customValue()}\"")

        val deserialized = DefaultJsonMapper.fromJson(json, JaNeiVetIkke::class.java)

        assertThat(deserialized).isEqualTo(jaNeiVetIkke)

        assertThat(DefaultJsonMapper.fromJson("\"${jaNeiVetIkke.name}\"", JaNeiVetIkke::class.java)).isEqualTo(jaNeiVetIkke)
    }

}
