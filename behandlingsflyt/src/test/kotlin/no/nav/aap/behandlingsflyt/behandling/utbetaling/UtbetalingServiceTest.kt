package no.nav.aap.behandlingsflyt.behandling.utbetaling

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9Repository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentGradering
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.tilkjentytelse.MeldeperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class UtbetalingServiceTest {


    val søknadsDato = LocalDate.of(2025, 1, 1)
    val førsteTilkjentYtelsePeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 13))
    val andreTilkjentYtelsePeriode = Periode(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 28))
    val tredjeTilkjentYtelsePeriode = Periode(LocalDate.of(2025, 1, 29), LocalDate.of(2025, 2, 9))

    @Test
    fun `skal utlede nye meldeperioder i tilkjent ytelse basert på hva som er utbetalt tidligere`() {
        val sakRepository = mockk<SakRepository>()
        val behandlingRepository = mockk<BehandlingRepository>()
        val avventUtbetalingService = mockk<AvventUtbetalingService>(relaxed = true)
        val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
        val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>(relaxed = true)
        val vedtakRepository = mockk<VedtakRepository>()
        val underveisRepository = mockk<UnderveisRepository>()
        val reduksjon11_9Repository = mockk<Reduksjon11_9Repository>(relaxed = true)

        every { sakRepository.hent(any<SakId>()) } returns sak
        every { behandlingRepository.hent(førstegangsbehandling.id) } returns førstegangsbehandling
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { behandlingRepository.hentAlleFor(any<SakId>()) } returns emptyList()

        every { vedtakRepository.hent(førstegangsbehandling.id) } returns Vedtak(
            førstegangsbehandling.id,
            andreTilkjentYtelsePeriode.tom.plusDays(1).atStartOfDay(), søknadsDato
        )
        every { vedtakRepository.hent(revurdering.id) } returns Vedtak(
            revurdering.id,
            tredjeTilkjentYtelsePeriode.tom.plusDays(1).atStartOfDay(),
            søknadsDato
        )

        every { tilkjentYtelseRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns tilkjentYtelseForFørstegangsbehandling()
        every { tilkjentYtelseRepository.hentHvisEksisterer(revurdering.id) } returns tilkjentYtelseForRevurdering()

        val utbetalingService = UtbetalingService(
            sakRepository = sakRepository,
            behandlingRepository = behandlingRepository,
            avventUtbetalingService = avventUtbetalingService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            avklaringsbehovRepository = avklaringsbehovRepository,
            vedtakRepository = vedtakRepository,
            underveisRepository = underveisRepository,
            reduksjon11_9Repository = reduksjon11_9Repository
        )

        val tilkjentYtelseDtoFørstegangsbehandling = utbetalingService.lagTilkjentYtelseForUtbetaling(sak.id, førstegangsbehandling.id)
        assertThat(tilkjentYtelseDtoFørstegangsbehandling).isNotNull
        assertThat(tilkjentYtelseDtoFørstegangsbehandling?.nyMeldeperiode).isNotNull
        assertThat(tilkjentYtelseDtoFørstegangsbehandling?.nyMeldeperiode).isEqualTo(MeldeperiodeDto(førsteTilkjentYtelsePeriode.fom, andreTilkjentYtelsePeriode.tom))

        val tilkjentYtelseDtoRevurdering = utbetalingService.lagTilkjentYtelseForUtbetaling(sak.id, revurdering.id)
        assertThat(tilkjentYtelseDtoRevurdering).isNotNull
        assertThat(tilkjentYtelseDtoRevurdering?.nyMeldeperiode).isNotNull
        assertThat(tilkjentYtelseDtoRevurdering?.nyMeldeperiode).isEqualTo(MeldeperiodeDto(tredjeTilkjentYtelsePeriode.fom, tredjeTilkjentYtelsePeriode.tom))


    }

    private fun tilkjentYtelseForFørstegangsbehandling(): List<TilkjentYtelsePeriode> = listOf(
        TilkjentYtelsePeriode(
            periode = førsteTilkjentYtelsePeriode,
            tilkjent = tilkjentYtelseDto(andreTilkjentYtelsePeriode.tom)
        ),
        TilkjentYtelsePeriode(
            periode = andreTilkjentYtelsePeriode,
            tilkjent = tilkjentYtelseDto(andreTilkjentYtelsePeriode.tom)
        )
    )

    private fun tilkjentYtelseForRevurdering(): List<TilkjentYtelsePeriode> = listOf(
        TilkjentYtelsePeriode(
            periode = førsteTilkjentYtelsePeriode,
            tilkjent = tilkjentYtelseDto(andreTilkjentYtelsePeriode.tom)
        ),
        TilkjentYtelsePeriode(
            periode = andreTilkjentYtelsePeriode,
            tilkjent = tilkjentYtelseDto(andreTilkjentYtelsePeriode.tom)
        ),
        TilkjentYtelsePeriode(
            periode = tredjeTilkjentYtelsePeriode,
            tilkjent = tilkjentYtelseDto(tredjeTilkjentYtelsePeriode.tom)
        ),

            )

    private fun tilkjentYtelseDto(utbetalingsdato: LocalDate): Tilkjent {
        return Tilkjent(
            dagsats = Beløp(100),
            gradering = TilkjentGradering(
                endeligGradering = Prosent.`100_PROSENT`,
                samordningGradering = Prosent.`0_PROSENT`,
                institusjonGradering = Prosent.`0_PROSENT`,
                arbeidGradering = Prosent.`0_PROSENT`,
                samordningUføregradering = Prosent.`0_PROSENT`,
                samordningArbeidsgiverGradering = Prosent.`0_PROSENT`
            ),
            grunnlagsfaktor = GUnit(1),
            grunnbeløp = Beløp(100),
            antallBarn = 0,
            barnetilleggsats = Beløp(40),
            barnetillegg = Beløp(0),
            utbetalingsdato = utbetalingsdato
        )
    }

    val sak = Sak(
        id = SakId(1),
        saksnummer = Saksnummer("1"),
        person = Person(
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("0".repeat(11)))
        ),
        rettighetsperiode = Periode(søknadsDato, søknadsDato.plusYears(1)),
    )
    val førstegangsbehandling = Behandling(
        id = BehandlingId(1),
        forrigeBehandlingId = null,
        sakId = sak.id,
        status = Status.AVSLUTTET,
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        årsakTilOpprettelse = null,
        versjon = 1,
    )
    val revurdering = Behandling(
        id = BehandlingId(2),
        forrigeBehandlingId = BehandlingId(1),
        sakId = sak.id,
        status = Status.AVSLUTTET,
        typeBehandling = TypeBehandling.Revurdering,
        årsakTilOpprettelse = null,
        versjon = 1,
    )
}