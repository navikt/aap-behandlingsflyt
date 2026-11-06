package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InMemoryTilkjentYtelseRepositoryTest {
    @Test
    fun `backfill oppdaterer bare redusert dagsats for perioder med stabile unike ID-er`() {
        val repository = InMemoryTilkjentYtelseRepository
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val dato = LocalDate.of(2024, 1, 1)
        val tilkjent = Tilkjent(
            dagsats = Beløp(1000),
            gradering = Prosent.`50_PROSENT`,
            graderingGrunnlag = GraderingGrunnlag(
                Prosent.`0_PROSENT`,
                Prosent.`0_PROSENT`,
                Prosent.`50_PROSENT`,
                Prosent.`0_PROSENT`,
                Prosent.`0_PROSENT`,
                Prosent.`0_PROSENT`,
            ),
            grunnlagsfaktor = GUnit("1.0"),
            grunnbeløp = Beløp(100000),
            antallBarn = 1,
            barnetilleggsats = Beløp(38),
            barnetillegg = Beløp(38),
            barnepensjonDagsats = Beløp(100),
            utbetalingsdato = dato.plusDays(14),
            minsteSats = Minstesats.IKKE_MINSTESATS,
            redusertDagsats = null,
        )
        val perioder = tidslinjeOf(
            Periode(dato, dato.plusDays(1)) to tilkjent,
            Periode(dato.plusDays(2), dato.plusDays(3)) to tilkjent,
            Periode(dato.plusDays(4), dato.plusDays(5)) to tilkjent.copy(redusertDagsats = Beløp(419))
        )
        repository.lagre(behandling.id, perioder)

        val kandidater = repository.hentTilkjentUtenRedusertDagsats(behandling.id)
        assertThat(kandidater).hasSize(2)
        assertThat(kandidater.map { it.tilkjentPeriodeId }).doesNotHaveDuplicates()
        assertThat(repository.hentTilkjentUtenRedusertDagsats(behandling.id)).isEqualTo(kandidater)

        kandidater[0].redusertDagsats = Beløp(321)
        kandidater[1].redusertDagsats = Beløp(432)
        assertThat(repository.hentHvisEksisterer(behandling.id)).isEqualTo(perioder)
        repository.backfillRedusertDagsats(kandidater)

        val segmenter = perioder.segmenter().toList()
        assertThat(repository.hentHvisEksisterer(behandling.id)).isEqualTo(
            Tidslinje(
                listOf(
                    segmenter[0].copy(verdi = tilkjent.copy(redusertDagsats = Beløp(321))),
                    segmenter[1].copy(verdi = tilkjent.copy(redusertDagsats = Beløp(432))),
                    segmenter[2],
                )
            )
        )
        assertThat(repository.hentTilkjentUtenRedusertDagsats(behandling.id)).isEmpty()
    }
}
