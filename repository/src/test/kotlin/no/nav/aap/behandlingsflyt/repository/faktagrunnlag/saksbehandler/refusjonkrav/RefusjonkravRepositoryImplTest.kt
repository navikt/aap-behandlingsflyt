package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
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
        val vurdering = RefusjonkravVurdering(
            harKrav = true,
            fom = periode.fom,
            tom = periode.tom,
            vurdertAv = "saksbehandler",
        )
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).lagre(
                sak.id, behandling.id, vurdering
            )
        }

        val uthentet = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(uthentet?.harKrav).isEqualTo(vurdering.harKrav)
        assertThat(uthentet?.fom).isEqualTo(vurdering.fom)
        assertThat(uthentet?.tom).isEqualTo(vurdering.tom)
        assertThat(uthentet?.vurdertAv).isEqualTo(vurdering.vurdertAv)

        // Lagre ny vurdering
        val vurdering2 = vurdering.copy(harKrav = false)
        dataSource.transaction {
            RefusjonkravRepositoryImpl(it).lagre(
                sak.id, behandling.id, vurdering2
            )
        }
        val uthentet2 = dataSource.transaction {
            RefusjonkravRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(uthentet2?.harKrav).isEqualTo(vurdering2.harKrav)
        assertThat(uthentet2?.fom).isEqualTo(vurdering2.fom)
        assertThat(uthentet2?.tom).isEqualTo(vurdering2.tom)
        assertThat(uthentet2?.vurdertAv).isEqualTo(vurdering2.vurdertAv)

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
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                AvklaringsbehovRepositoryImpl(connection),
                TrukketSøknadRepositoryImpl(connection)
            ),
        ).finnEllerOpprett(ident(), Periode(1 januar 2022, 31.desember(2023)))
    }
}