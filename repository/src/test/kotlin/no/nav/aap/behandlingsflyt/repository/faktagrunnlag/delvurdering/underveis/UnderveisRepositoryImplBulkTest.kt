package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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

class UnderveisRepositoryImplBulkTest {

    private lateinit var dataSource: TestDataSource
    private val gatewayProvider = minimalGatewayProvider()

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `hentBulk returnerer grunnlag for kjente behandlinger og ignorerer ukjente`() {
        val sak1 = dataSource.transaction { sak(it, 1 januar 2025) }
        val sak2 = dataSource.transaction { sak(it, 1 januar 2025) }
        val periode = Periode(1 januar 2025, 31 januar 2025)

        val behandlingMedRett = dataSource.transaction { finnEllerOpprettBehandling(it, sak1, gatewayProvider = gatewayProvider) }
        val behandlingUtenRett = dataSource.transaction { finnEllerOpprettBehandling(it, sak2, gatewayProvider = gatewayProvider) }
        val ukjentId = BehandlingId(Long.MAX_VALUE)

        dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).lagre(behandlingMedRett.id, listOf(oppfyltPeriode(periode)), object : Faktagrunnlag {})
            UnderveisRepositoryImpl(connection).lagre(behandlingUtenRett.id, listOf(ikkeOppfyltPeriode(periode)), object : Faktagrunnlag {})
        }

        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentBulk(
                listOf(behandlingMedRett.id, behandlingUtenRett.id, ukjentId)
            )
        }

        assertThat(resultat).containsOnlyKeys(behandlingMedRett.id, behandlingUtenRett.id)
        assertThat(resultat[behandlingMedRett.id]?.harRett()).isTrue()
        assertThat(resultat[behandlingUtenRett.id]?.harRett()).isFalse()
    }

    @Test
    fun `hentBulk med tom liste returnerer tomt map uten DB-kall`() {
        val resultat = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hentBulk(emptyList())
        }

        assertThat(resultat).isEmpty()
    }

    private fun oppfyltPeriode(periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = null,
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
        meldepliktStatus = null,
        meldepliktGradering = null,
    )

    private fun ikkeOppfyltPeriode(periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.IKKE_OPPFYLT,
        rettighetsType = null,
        avslagsårsak = UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
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
        meldepliktStatus = null,
        meldepliktGradering = null,
    )
}
