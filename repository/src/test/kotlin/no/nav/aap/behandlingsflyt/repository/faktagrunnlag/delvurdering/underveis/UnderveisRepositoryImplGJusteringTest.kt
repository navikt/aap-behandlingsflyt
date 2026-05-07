package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class UnderveisRepositoryImplGJusteringTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    private val gJusteringsdato = LocalDate.of(2025, 5, 1)

    @Test
    fun `returnerer sak med oppfylt underveisperiode som inneholder g-justeringsdato`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2025) }
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            VedtakService(postgresRepositoryRegistry.provider(connection))
                .lagreVedtak(behandling.id, LocalDateTime.now(), gJusteringsdato)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = behandling.id,
                underveisperioder = listOf(
                    oppfyltPeriode(Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 31)))
                ),
                input = object : Faktagrunnlag {},
            )
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentSakerForGRegulering(gJusteringsdato)
        }

        assertThat(resultat).containsExactly(sak.id)
    }

    @Test
    fun `returnerer ikke sak når g-justeringsdatoen er etter alle underveisperioder`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2025) }
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            VedtakService(postgresRepositoryRegistry.provider(connection))
                .lagreVedtak(behandling.id, LocalDateTime.now(), gJusteringsdato)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = behandling.id,
                underveisperioder = listOf(
                    oppfyltPeriode(Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 30)))
                ),
                input = object : Faktagrunnlag {},
            )
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentSakerForGRegulering(gJusteringsdato)
        }

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `returnerer ikke sak der perioden som inneholder datoen har utfall ikke-oppfylt`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2025) }
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            VedtakService(postgresRepositoryRegistry.provider(connection))
                .lagreVedtak(behandling.id, LocalDateTime.now(), gJusteringsdato)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = behandling.id,
                underveisperioder = listOf(
                    ikkeOppfyltPeriode(Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 31)))
                ),
                input = object : Faktagrunnlag {},
            )
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentSakerForGRegulering(gJusteringsdato)
        }

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `returnerer ikke sak uten vedtak, selv om underveisperiode dekker g-justeringsdatoen`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2025) }
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            // Ingen vedtak — behandlingen vises ikke i gjeldende_vedtatte_behandlinger
            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = behandling.id,
                underveisperioder = listOf(
                    oppfyltPeriode(Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 31)))
                ),
                input = object : Faktagrunnlag {},
            )
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentSakerForGRegulering(gJusteringsdato)
        }

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `returnerer kun saker med treff, ikke alle saker`() {
        val sakMedTreff = dataSource.transaction { sak(it, 1 januar 2025) }
        val sakUtenTreff = dataSource.transaction { sak(it, 1 januar 2025) }

        dataSource.transaction { connection ->
            val behandlingMedTreff = finnEllerOpprettBehandling(connection, sakMedTreff)
            VedtakService(postgresRepositoryRegistry.provider(connection))
                .lagreVedtak(behandlingMedTreff.id, LocalDateTime.now(), gJusteringsdato)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandlingMedTreff.id, Status.AVSLUTTET)
            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = behandlingMedTreff.id,
                underveisperioder = listOf(
                    oppfyltPeriode(Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 31)))
                ),
                input = object : Faktagrunnlag {},
            )

            val behandlingUtenTreff = finnEllerOpprettBehandling(connection, sakUtenTreff)
            VedtakService(postgresRepositoryRegistry.provider(connection))
                .lagreVedtak(behandlingUtenTreff.id, LocalDateTime.now(), gJusteringsdato)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandlingUtenTreff.id, Status.AVSLUTTET)
            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = behandlingUtenTreff.id,
                underveisperioder = listOf(
                    oppfyltPeriode(Periode(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 7, 31)))
                ),
                input = object : Faktagrunnlag {},
            )
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentSakerForGRegulering(gJusteringsdato)
        }

        assertThat(resultat).containsExactly(sakMedTreff.id)
    }

    @Test
    fun `returnerer sak kun én gang selv om flere perioder inneholder g-justeringsdatoen`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2025) }
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak)
            VedtakService(postgresRepositoryRegistry.provider(connection))
                .lagreVedtak(behandling.id, LocalDateTime.now(), gJusteringsdato)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            UnderveisRepositoryImpl(connection).lagre(
                behandlingId = behandling.id,
                underveisperioder = listOf(
                    oppfyltPeriode(Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 5, 15))),
                    oppfyltPeriode(Periode(LocalDate.of(2025, 5, 16), LocalDate.of(2025, 6, 30))),
                ),
                input = object : Faktagrunnlag {},
            )
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentSakerForGRegulering(gJusteringsdato)
        }

        assertThat(resultat).containsExactly(sak.id)
    }

    private fun oppfyltPeriode(periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = null,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = `0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = `0_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = setOf(Kvote.ORDINÆR),
        institusjonsoppholdReduksjon = `0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = `0_PROSENT`,
    )

    private fun ikkeOppfyltPeriode(periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.IKKE_OPPFYLT,
        rettighetsType = null,
        avslagsårsak = UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = `0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = `0_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        institusjonsoppholdReduksjon = `0_PROSENT`,
        meldepliktStatus = null,
        meldepliktGradering = null,
    )
}
