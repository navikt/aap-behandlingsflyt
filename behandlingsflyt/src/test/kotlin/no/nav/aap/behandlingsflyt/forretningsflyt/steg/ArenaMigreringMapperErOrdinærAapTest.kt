package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.arena.ArenaDiagnose
import no.nav.aap.behandlingsflyt.arena.ArenaDiagnoseType
import no.nav.aap.behandlingsflyt.arena.ArenaSykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.arena.ArenaVilkar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaMigreringMapperErOrdinærAapTest {

    private fun response(vararg vilkar: ArenaVilkar) = ArenaSykdomsvurderingResponse(
        begrunnelse = "Begrunnelse",
        vilkar = vilkar.toList(),
        diagnoser = listOf(
            ArenaDiagnose(
                kodeverk = "ICD10",
                kode = "M797",
                type = ArenaDiagnoseType.HOVEDDIAGNOSE,
                opprettet = LocalDate.of(2016, 1, 1),
            )
        ),
    )

    @Test
    fun `erOrdinærAap er true når alle påkrevde vilkår er oppfylt`() {
        val fraArena = response(
            ArenaVilkar(kode = "INNTNEDS", oppfylt = true),
            ArenaVilkar(kode = "SYKSKADLYT", oppfylt = true),
            ArenaVilkar(kode = "AAARBEVNE", oppfylt = true),
        )

        assertThat(fraArena.erOrdinærAap()).isTrue()
    }

    @Test
    fun `erOrdinærAap er false når ett påkrevd vilkår mangler`() {
        val fraArena = response(
            ArenaVilkar(kode = "INNTNEDS", oppfylt = true),
            ArenaVilkar(kode = "SYKSKADLYT", oppfylt = true),
        )

        assertThat(fraArena.erOrdinærAap()).isFalse()
    }

    @Test
    fun `erOrdinærAap er false når ett påkrevd vilkår er tilstede men ikke oppfylt`() {
        val fraArena = response(
            ArenaVilkar(kode = "INNTNEDS", oppfylt = true),
            ArenaVilkar(kode = "SYKSKADLYT", oppfylt = true),
            ArenaVilkar(kode = "AAARBEVNE", oppfylt = false),
        )

        assertThat(fraArena.erOrdinærAap()).isFalse()
    }

    @Test
    fun `erOrdinærAap ignorerer vilkår som ikke er relevante for ordinær AAP`() {
        val fraArena = response(
            ArenaVilkar(kode = "INNTNEDS", oppfylt = true),
            ArenaVilkar(kode = "SYKSKADLYT", oppfylt = true),
            ArenaVilkar(kode = "AAARBEVNE", oppfylt = true),
            ArenaVilkar(kode = "ANNET_VILKÅR", oppfylt = false),
        )

        assertThat(fraArena.erOrdinærAap()).isTrue()
    }
}
