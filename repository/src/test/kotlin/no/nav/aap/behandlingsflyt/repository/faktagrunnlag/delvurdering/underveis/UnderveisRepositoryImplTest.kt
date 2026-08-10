package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
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

class UnderveisRepositoryImplTest {

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
    fun `andelArbeid leses fra lagret verdi og ikke fra utledning basert paa gradering`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2025) }
        val periode = Periode(1 januar 2025, 31 januar 2025)
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak, gatewayProvider = gatewayProvider) }

        // andelArbeid=0% mens gradering=0%. Tidligere utledning (100% - gradering) ville
        // feilaktig gitt 100%. Nå skal den lagrede verdien 0% leses tilbake.
        dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).lagre(behandling.id, listOf(oppfyltPeriode(periode)), object : Faktagrunnlag {})
        }

        val grunnlag = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(behandling.id)
        }

        assertThat(grunnlag.perioder.single().arbeidsgradering.andelArbeid).isEqualTo(`0_PROSENT`)
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
}
