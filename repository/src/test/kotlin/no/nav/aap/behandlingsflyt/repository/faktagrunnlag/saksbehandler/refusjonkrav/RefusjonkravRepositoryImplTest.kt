package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RefusjonkravRepositoryImplTest {
    @Test
    fun `lagre, hent og slett`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val sak = dataSource.transaction { sak(it) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }


        val periode = Periode(1 januar 2022, 31.desember(2023))
        val vurderinger = listOf(
            RefusjonkravVurdering(
                harKrav = true,
                fom = periode.fom,
                tom = periode.tom,
                vurdertAv = "saksbehandler",
                navKontor = "",
            )
        )
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).lagre(
                sak.id, behandling.id, vurderinger
            )
        }

        val uthentet = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet).hasSameSizeAs(vurderinger)
        uthentet!!.zip(vurderinger).forEach { (actual, expected) ->
            assertThat(actual.harKrav).isEqualTo(expected.harKrav)
            assertThat(actual.fom).isEqualTo(expected.fom)
            assertThat(actual.tom).isEqualTo(expected.tom)
            assertThat(actual.vurdertAv).isEqualTo(expected.vurdertAv)
        }
        // Lagre ny vurdering
        val vurderinger2 = listOf(vurderinger.first().copy(harKrav = false))
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).lagre(
                sak.id, behandling.id, vurderinger2
            )
        }
        val uthentet2 = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        uthentet2!!.zip(vurderinger2).forEach { (actual, expected) ->
            assertThat(actual.harKrav).isEqualTo(expected.harKrav)
            assertThat(actual.fom).isEqualTo(expected.fom)
            assertThat(actual.tom).isEqualTo(expected.tom)
            assertThat(actual.vurdertAv).isEqualTo(expected.vurdertAv)
        }
        // SLETT
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).slett(behandling.id)
        }
        val uthentetEtterSletting = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(uthentetEtterSletting).isNull()
    }


    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(1 januar 2022, 31.desember(2023)))
    }
}