package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_VEDTAKSLENGDE_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvklaringsbehovLøsningTest {
    @Test
    fun `alle subtyper skal ha behovstype field `() {
        val løsningSubtypes = utledSubtypesTilAvklaringsbehovLøsning()

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
        val løsningSubtypes = utledSubtypesTilAvklaringsbehovLøsning()

        assertThat(løsningSubtypes).allMatch { it.isAnnotationPresent(JsonTypeName::class.java) }
    }

    @Test
    fun `subtyper skal ha unik verdi på annotasjonen unntatt vedtakslengdeovergangen`() {
        val utledSubtypes = utledSubtypesTilAvklaringsbehovLøsning()
        val dupliserteKoder = utledSubtypes
            .groupBy { it.getAnnotation(JsonTypeName::class.java).value }
            .filterValues { it.size > 1 }

        assertThat(dupliserteKoder.keys).containsExactly(AVKLAR_VEDTAKSLENGDE_KODE)
        assertThat(dupliserteKoder.getValue(AVKLAR_VEDTAKSLENGDE_KODE)).containsExactlyInAnyOrder(
            AvklarVedtakslengdeLøsning::class.java,
            AvklarVedtakslengdePeriodisertLøsning::class.java
        )
    }

    @Test
    fun `alle subtyper skal ha tilhørende definisjon`() {
        val utledSubtypes = utledSubtypesTilAvklaringsbehovLøsning()
        val løsningSubtypes =
            utledSubtypes.map { Definisjon.forKode(it.getAnnotation(JsonTypeName::class.java).value) }

        assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
    }
}