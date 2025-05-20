package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.refusjonskrav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
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

class TjenestepensjonRefusjonskravVurderingRepositoryImplTest {
    @Test
    fun `lagre, hente ut igjen, slette`() {
        val dataSource = InitTestDatabase.freshDatabase()

        val sak = dataSource.transaction { sak(it) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val periode = Periode(1 januar 2022, 31.desember(2023))
        val vurdering = TjenestepensjonRefusjonskravVurdering(
            harKrav = true,
            fom = periode.fom,
            tom = periode.tom,
            begrunnelse = "dasdasd"
        )
        dataSource.transaction {
            TjenestepensjonRefusjonskravVurderingRepositoryImpl(it).lagre(
                sak.id, behandling.id,
                vurdering
            )
        }

        val uthentet = dataSource.transaction {
            TjenestepensjonRefusjonskravVurderingRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(uthentet).isEqualTo(vurdering)

        // SLETT

        dataSource.transaction {
            TjenestepensjonRefusjonskravVurderingRepositoryImpl(it).slett(behandling.id)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(1 januar 2022, 31.desember(2023)))
    }
}