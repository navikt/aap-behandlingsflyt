package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FreshDatabaseExtension
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(FreshDatabaseExtension::class)
class VurderRettighetsperiodeRepositoryImplTest(val dataSource: DataSource) {

    @Test
    fun `skal lagre vurdering av rettighetsperiode`() {
        dataSource.transaction { connection ->
            val repo = VurderRettighetsperiodeRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurderingerFørLagring = repo.hentVurdering(behandling.id)

            val vurdering = RettighetsperiodeVurdering(
                startDato = LocalDate.now().minusDays(10),
                begrunnelse = "begrunnelse",
                harRettUtoverSøknadsdato = true,
                harKravPåRenter = true,
                vurdertAv = "NAVident"
            )
            repo.lagreVurdering(behandling.id, vurdering)

            val vurderingerEtterLagring = repo.hentVurdering(behandling.id)
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
            val repo = VurderRettighetsperiodeRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurderingerFørLagring = repo.hentVurdering(behandling.id)

            val vurdering = RettighetsperiodeVurdering(
                startDato = null,
                begrunnelse = "begrunnelse",
                harRettUtoverSøknadsdato = false,
                harKravPåRenter = null,
                vurdertAv = "NAVident"
            )
            repo.lagreVurdering(behandling.id, vurdering)

            val vurderingerEtterLagring = repo.hentVurdering(behandling.id)
            assertThat(vurderingerFørLagring).isNull()
            assertThat(vurderingerEtterLagring).isNotNull()
            assertThat(vurderingerEtterLagring)
                .usingRecursiveComparison()
                .ignoringFields("vurdertDato")
                .isEqualTo(vurdering)
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