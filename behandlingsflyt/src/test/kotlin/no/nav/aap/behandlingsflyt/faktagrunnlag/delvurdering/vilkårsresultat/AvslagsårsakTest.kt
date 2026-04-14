package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.kontrakt.datadeling.AvslagsårsakDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvslagsårsakTest {
    @Test
    fun `type avslags matcher dto-versjonen`() {
        for (avslagsårsak in Avslagsårsak.entries) {
            val dto = when (avslagsårsak.avslagstype) {
                Avslagstype.STANS, Avslagstype.OPPHØR -> AvslagsårsakDTO.valueOf(avslagsårsak.toString())
                Avslagstype.KUN_INNGANGSVILKÅR, Avslagstype.UKJENT -> continue
            }
            assertThat(avslagsårsak.avslagstype.toString())
                .isEqualTo(dto.type.toString())
        }
    }
}