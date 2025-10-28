package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.dbtest.TestDataSource.Companion.invoke
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VurderRettighetsperiodeRepositoryImplTest {

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
    fun `skal lagre vurdering av rettighetsperiode`() {
        dataSource.transaction { connection ->
            val vurderRettighetsPeriodeRepo= VurderRettighetsperiodeRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurderingerFørLagring = vurderRettighetsPeriodeRepo.hentVurdering(behandling.id)

            val vurdering = RettighetsperiodeVurdering(
                startDato = LocalDate.now().minusDays(10),
                begrunnelse = "begrunnelse",
                harRettUtoverSøknadsdato = true,
                harKravPåRenter = true,
                vurdertAv = "NAVident"
            )
            vurderRettighetsPeriodeRepo.lagreVurdering(behandling.id, vurdering)

            val vurderingerEtterLagring = vurderRettighetsPeriodeRepo.hentVurdering(behandling.id)
            assertThat(vurderingerFørLagring).isNull()
            assertThat(vurderingerEtterLagring).isNotNull()
            assertThat(vurderingerEtterLagring)
                .usingRecursiveComparison()
                .ignoringFields("vurdertDato")
                .isEqualTo(vurdering)
        }
    }

    @Test
    fun `skal lagre vurdering av rettighetsperiode uten ny startdato`() {
        dataSource.transaction { connection ->
            val vurderRettighetsPeriodeRepo = VurderRettighetsperiodeRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurderingerFørLagring = vurderRettighetsPeriodeRepo.hentVurdering(behandling.id)

            val vurdering = RettighetsperiodeVurdering(
                startDato = null,
                begrunnelse = "begrunnelse",
                harRettUtoverSøknadsdato = false,
                harKravPåRenter = null,
                vurdertAv = "NAVident"
            )
            vurderRettighetsPeriodeRepo.lagreVurdering(behandling.id, vurdering)

            val vurderingerEtterLagring = vurderRettighetsPeriodeRepo.hentVurdering(behandling.id)
            assertThat(vurderingerFørLagring).isNull()
            assertThat(vurderingerEtterLagring).isNotNull()
            assertThat(vurderingerEtterLagring)
                .usingRecursiveComparison()
                .ignoringFields("vurdertDato")
                .isEqualTo(vurdering)
        }
    }

    @Test
    fun `skal deaktivere vurdering av rettighetsperiode`() {
        dataSource.transaction { connection ->
            val vurderRettighetsPeriodeRepo = VurderRettighetsperiodeRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurdering = RettighetsperiodeVurdering(
                startDato = LocalDate.now().minusDays(10),
                begrunnelse = "begrunnelse",
                harRettUtoverSøknadsdato = true,
                harKravPåRenter = true,
                vurdertAv = "NAVident"
            )

            vurderRettighetsPeriodeRepo.lagreVurdering(behandling.id, vurdering)

            val lagretVurdering = vurderRettighetsPeriodeRepo.hentVurdering(behandling.id)
            assertThat(lagretVurdering).isNotNull()

            vurderRettighetsPeriodeRepo.lagreVurdering(behandling.id, null)

            val lagretVurderingEtterDeaktivering = vurderRettighetsPeriodeRepo.hentVurdering(behandling.id)
            assertThat(lagretVurderingEtterDeaktivering).isNull()
        }
    }

    private companion object {
        private fun sak(connection: DBConnection): Sak {
            return PersonOgSakService(
                FakePdlGateway,
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(ident(), periode)
        }

        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}