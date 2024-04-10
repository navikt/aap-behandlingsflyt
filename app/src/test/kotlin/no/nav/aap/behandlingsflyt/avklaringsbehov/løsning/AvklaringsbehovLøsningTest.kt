package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvklaringsbehovLøsningTest {
    @Test
    fun `alle subtyper skal ha behovstype field `() {
        val løsningSubtypes = utledSubtypes()

        assertThat(løsningSubtypes).allMatch { validerAlleKlasserHarFelt(it) }
    }

    private fun validerAlleKlasserHarFelt(it: Class<*>): Boolean {
        val felter = it.declaredFields

        if (felter.size < 1) {
            return false
        }

        val harFeltAvType = felter.any { felt ->
            felt.name == "behovstype" && (!felt.isAnnotationPresent(JsonProperty::class.java) || felt.getAnnotation(
                JsonProperty::class.java
            ).value == "behovstype")
        }
        if (harFeltAvType) {
            return true
        }
        return felter.any { felt ->
            felt.isAnnotationPresent(JsonProperty::class.java) && felt.getAnnotation(
                JsonProperty::class.java
            ).value == "behovstype"
        }
    }

    @Test
    fun `alle subtyper skal ha annotasjon`() {
        val løsningSubtypes = utledSubtypes()

        assertThat(løsningSubtypes).allMatch { it.isAnnotationPresent(JsonTypeName::class.java) }
    }

    @Test
    fun `alle subtyper skal ha unik verdi på annotasjonen`() {
        val utledSubtypes = utledSubtypes()
        val løsningSubtypes = utledSubtypes.map { it.getAnnotation(JsonTypeName::class.java).value }.toSet()

        assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
    }

    @Test
    fun `alle subtyper skal ha tilhørende definisjon`() {
        val utledSubtypes = utledSubtypes()
        val løsningSubtypes =
            utledSubtypes.map { Definisjon.forKode(it.getAnnotation(JsonTypeName::class.java).value) }.toSet()

        assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
    }
}