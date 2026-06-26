package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UnderveisRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `kan lagre og hente underveisperioder`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val underveisperioder = listOf(
                oppfyltPeriode(Periode(1 januar 2025, 14 januar 2025)),
                ikkeOppfyltPeriode(Periode(15 januar 2025, 31 januar 2025)),
            )

            val repository = UnderveisRepositoryImpl(connection)
            repository.lagre(behandling.id, underveisperioder, object : Faktagrunnlag {})

            val hentet = repository.hent(behandling.id)

            assertThat(hentet.perioder).containsExactlyElementsOf(underveisperioder)
        }
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
            andelArbeid = Prosent.`0_PROSENT`,
            fastsattArbeidsevne = Prosent.`0_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = 15 januar 2025,
        ),
        trekk = Dagsatser(1),
        brukerAvKvoter = setOf(Kvote.ORDINÆR),
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.MELDT_SEG,
        meldepliktGradering = Prosent.`0_PROSENT`,
    )

    private fun ikkeOppfyltPeriode(periode: Periode) = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.IKKE_OPPFYLT,
        rettighetsType = null,
        avslagsårsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
            andelArbeid = Prosent.`100_PROSENT`,
            fastsattArbeidsevne = Prosent.`0_PROSENT`,
            gradering = Prosent.`0_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        meldepliktStatus = MeldepliktStatus.IKKE_MELDT_SEG,
        meldepliktGradering = Prosent.`100_PROSENT`,
    )
}
