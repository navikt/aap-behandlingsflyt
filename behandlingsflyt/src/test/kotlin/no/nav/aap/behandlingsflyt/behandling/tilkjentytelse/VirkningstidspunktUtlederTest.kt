package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class VirkningstidspunktUtlederTest {
    @Test
    fun `full gradering, ingen samordning`() {
        val behandlingId = opprettBehandling().id
        val utleder = VirkningstidspunktUtleder(
            underveisRepository = InMemoryUnderveisRepository,
            samordningRepository = InMemorySamordningRepository,
            tilkjentYtelseRepository = InMemoryTilkjentYtelseRepository
        )
        val periode = Periode(1 februar 2024, 15 februar 2024)

        InMemoryUnderveisRepository.lagre(
            behandlingId, listOf(
                Underveisperiode.oppfylt(periode)
            ), object : Faktagrunnlag {})
        InMemorySamordningRepository.lagre(behandlingId, emptyList(), object : Faktagrunnlag {})

        InMemoryTilkjentYtelseRepository.lagre(
            behandlingId, listOf(
                TilkjentYtelsePeriode(
                    periode, Tilkjent(
                        dagsats = Beløp("954.06"),
                        gradering = TilkjentGradering(Prosent.`100_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`),
                        grunnlag = Beløp("954.06"),
                        grunnlagsfaktor = GUnit("0.0076923077"),
                        grunnbeløp = Beløp("124028"),
                        antallBarn = 0,
                        barnetilleggsats = Beløp("0"),
                        barnetillegg = Beløp("0"),
                        utbetalingsdato = periode.tom.plusDays(1)
                    )
                )
            )
        )

        assertThat(utleder.utledVirkningsTidspunkt(behandlingId)).isEqualTo(periode.fom)
    }

    @Test
    fun `vilkår oppfylt, men samordning overlapper i begynnelsen`() {
        val behandlingId = opprettBehandling().id
        val utleder = VirkningstidspunktUtleder(
            underveisRepository = InMemoryUnderveisRepository,
            samordningRepository = InMemorySamordningRepository,
            tilkjentYtelseRepository = InMemoryTilkjentYtelseRepository
        )
        val periode = Periode(1 februar 2024, 28 februar 2024)
        val samordningPeriode = Periode(1 januar 2024, 15 februar 2024)

        InMemoryUnderveisRepository.lagre(
            behandlingId, listOf(
                Underveisperiode.oppfylt(periode)
            ), object : Faktagrunnlag {})

        // Lagrer 100% samordning i begynnelsen
        InMemorySamordningRepository.lagre(
            behandlingId,
            listOf(SamordningPeriode(samordningPeriode, Prosent.`100_PROSENT`)),
            object : Faktagrunnlag {})

        InMemoryTilkjentYtelseRepository.lagre(
            behandlingId, listOf(
                TilkjentYtelsePeriode(
                    periode, Tilkjent(
                        dagsats = Beløp("954.06"),
                        gradering = TilkjentGradering(Prosent.`100_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`, Prosent.`0_PROSENT`),
                        grunnlag = Beløp("954.06"),
                        grunnlagsfaktor = GUnit("0.0076923077"),
                        grunnbeløp = Beløp("124028"),
                        antallBarn = 0,
                        barnetilleggsats = Beløp("0"),
                        barnetillegg = Beløp("0"),
                        utbetalingsdato = periode.tom.plusDays(1)
                    )
                )
            )
        )
        assertThat(utleder.utledVirkningsTidspunkt(behandlingId)).isEqualTo(16 februar 2024)
    }
}

fun opprettBehandling(): Behandling {
    val person = Person(1, UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))

    val sak = InMemorySakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    val behandling =
        InMemoryBehandlingRepository.opprettBehandling(sak.id, listOf(), TypeBehandling.Førstegangsbehandling, null)
    return behandling
}

private fun Underveisperiode.Companion.oppfylt(periode: Periode): Underveisperiode {
    return Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = null,
        grenseverdi = Prosent.`50_PROSENT`,
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = Prosent(0),
            fastsattArbeidsevne = Prosent.`0_PROSENT`,
            gradering = Prosent.`100_PROSENT`
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = setOf(),
        bruddAktivitetspliktId = null,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
    )
}
