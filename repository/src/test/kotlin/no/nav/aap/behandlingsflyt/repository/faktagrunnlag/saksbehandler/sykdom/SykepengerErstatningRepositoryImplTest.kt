package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.dbtest.TestDatabaseExtension
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(TestDatabaseExtension::class)
internal class SykepengerErstatningRepositoryImplTest {
    @TestDatabase
    lateinit var dataSource: DataSource

    @Test
    fun `lagre og hente ut igjen`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }
        val vurdering = SykepengerVurdering(
            begrunnelse = "yolo",
            dokumenterBruktIVurdering = listOf(JournalpostId("123"), JournalpostId("321")),
            harRettPå = true,
            grunn = null,
            vurdertAv = "saksbehandler"
        )
        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling.id, vurdering)
        }

        val res = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(res.vurdering).usingRecursiveComparison()
            .ignoringFields("vurdertTidspunkt")
            .isEqualTo(vurdering)
        assertThat(res.vurdering?.vurdertTidspunkt).isNotNull()

        // Lagre nytt, hent ut nyeste

        val vurdering2 = SykepengerVurdering(
            begrunnelse = "yolo x2",
            dokumenterBruktIVurdering = listOf(JournalpostId("456")),
            harRettPå = false,
            grunn = SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR,
            vurdertAv = "saksbehandler!!"
        )

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling.id, vurdering2)
        }

        val res2 = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hent(behandling.id)
        }
        assertThat(res2.vurdering).usingRecursiveComparison()
            .ignoringFields("vurdertTidspunkt")
            .isEqualTo(vurdering2)
        assertThat(res2.vurdering?.vurdertTidspunkt).isNotNull()

        // Test sletting

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).slett(behandling.id)
        }

        val res3 = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(res3).isNull()

    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    }
}