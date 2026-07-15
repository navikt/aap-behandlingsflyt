package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class UnderveisRepositoryImplAutomatiskMeldekortTest {

    private lateinit var dataSource: TestDataSource
    private val gatewayProvider = minimalGatewayProvider { }

    private val idag = LocalDate.of(2026, 6, 9)

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `returnerer ubesvarte meldeperioder for to saker`() {
        val meldeperiode1 = Periode(idag.minusDays(28), idag.minusDays(15))
        val meldeperiode2 = Periode(idag.minusDays(14), idag.minusDays(1))
        val sak1 = dataSource.transaction { sak(it, 1 januar 2025) }
        val sak2 = dataSource.transaction { sak(it, 1 januar 2025) }
        dataSource.transaction { connection ->
            for ((sak, meldeperiode) in listOf(sak1 to meldeperiode1, sak2 to meldeperiode2)) {
                val behandling = finnEllerOpprettBehandling(connection, sak, gatewayProvider = gatewayProvider)
                VedtakService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
                    .lagreVedtak(behandling.id, LocalDateTime.now(), idag.minusDays(30))
                BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
                UnderveisRepositoryImpl(connection).lagre(
                    behandlingId = behandling.id,
                    underveisperioder = listOf(ikkeMeldtSegPeriode(meldeperiode)),
                    input = object : Faktagrunnlag {},
                )
            }
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentUbesvarteMeldeperioderForDollyJobb(listOf(sak1.id, sak2.id), idag)
        }

        assertThat(resultat[sak1.id]).containsExactly(meldeperiode1)
        assertThat(resultat[sak2.id]).containsExactly(meldeperiode2)
    }

    private fun ikkeMeldtSegPeriode(meldeperiode: Periode) = Underveisperiode(
        periode = meldeperiode,
        meldePeriode = meldeperiode,
        utfall = Utfall.IKKE_OPPFYLT,
        rettighetsType = null,
        avslagsårsak = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
        grenseverdi = `100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = `0_PROSENT`,
            fastsattArbeidsevne = `100_PROSENT`,
            gradering = `0_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        institusjonsoppholdReduksjon = `0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.IKKE_MELDT_SEG,
        meldepliktGradering = `0_PROSENT`,
    )
}
