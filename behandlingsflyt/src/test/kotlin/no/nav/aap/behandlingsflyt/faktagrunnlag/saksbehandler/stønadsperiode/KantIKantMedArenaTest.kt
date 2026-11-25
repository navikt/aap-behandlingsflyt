package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.LocalDate

class KantIKantMedArenaTest {

    @ParameterizedTest
    @EnumSource(
        KantIKantUtfall::class,
        names = ["KANT_I_KANT", "ANNET_KRAV_ER_KANT_I_KANT", "OPPHOLD_ETTER_MAKSDATO", "INGEN_MAKSDATO"]
    )
    fun `utfall med Arena-sak krever peker`(utfall: KantIKantUtfall) {
        assertThatCode { KantIKantMedArena(utfall, arenaMaksdatoId = 1) }.doesNotThrowAnyException()
        assertThatThrownBy { KantIKantMedArena(utfall, arenaMaksdatoId = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest
    @EnumSource(KantIKantUtfall::class, names = ["INGEN_ARENASAK", "IKKE_VURDERT"])
    fun `utfall uten Arena-sak kan ikke ha peker`(utfall: KantIKantUtfall) {
        assertThatCode { KantIKantMedArena(utfall, arenaMaksdatoId = null) }.doesNotThrowAnyException()
        assertThatThrownBy { KantIKantMedArena(utfall, arenaMaksdatoId = 1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest
    @EnumSource(KantIKantUtfall::class)
    fun `erKantIKantMedArena må stemme med utfallet`(utfall: KantIKantUtfall) {
        val kantIKant = KantIKantMedArena(utfall, arenaMaksdatoId = if (utfall.harArenaSak) 1 else null)
        val erKantIKant = utfall == KantIKantUtfall.KANT_I_KANT

        assertThatCode { vurdering(erKantIKantMedArena = erKantIKant, kantIKant = kantIKant) }
            .doesNotThrowAnyException()
        assertThatThrownBy { vurdering(erKantIKantMedArena = !erKantIKant, kantIKant = kantIKant) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun vurdering(erKantIKantMedArena: Boolean, kantIKant: KantIKantMedArena) = StønadsperiodeVurdering(
        referanse = Kravreferanse.ny(),
        opprettet = Instant.now(),
        vurdertIBehandling = BehandlingId(1),
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        harHattOrdinærSiste52Uker = erKantIKantMedArena,
        harGjenværendeKvote = false,
        relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
        startDato = LocalDate.of(2024, 8, 2),
        erKantIKantMedArena = erKantIKantMedArena,
        kantIKant = kantIKant,
    )
}
