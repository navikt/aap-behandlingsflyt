package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RettTilBarnetilleggTest {
    @Test
    fun `legg til uavklart barn kun hvis det ikke finnes i folkeregisteret`() {
        val rettTilBarnetillegg = RettTilBarnetillegg()

        rettTilBarnetillegg.leggTilFolkeregisterBarn(setOf(BarnIdentifikator.BarnIdent("123")))

        assertThat(rettTilBarnetillegg.barnTilAvklaring().size).isEqualTo(0)

        rettTilBarnetillegg.leggTilOppgitteBarn(setOf(BarnIdentifikator.BarnIdent("123")))

        // Fortsatt 0
        assertThat(rettTilBarnetillegg.barnTilAvklaring().size).isEqualTo(0)

        // Legger til ukjent barn
        rettTilBarnetillegg.leggTilOppgitteBarn(setOf(BarnIdentifikator.BarnIdent("543")))

        // Nå skal det være ett barn til avklaring
        assertThat(rettTilBarnetillegg.barnTilAvklaring().size).isEqualTo(1)

    }
}