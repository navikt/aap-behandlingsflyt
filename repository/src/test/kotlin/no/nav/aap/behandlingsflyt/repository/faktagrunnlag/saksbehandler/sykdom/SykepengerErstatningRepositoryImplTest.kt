package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
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
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(FreshDatabaseExtension::class)
internal class SykepengerErstatningRepositoryImplTest(val dataSource: DataSource) {

    @Test
    fun `lagre og hente ut igjen`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }
        val vurdering = SykepengerVurdering(
            begrunnelse = "yolo",
            dokumenterBruktIVurdering = listOf(JournalpostId("123")),
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

        assertThat(res.vurdering?.begrunnelse).isEqualTo(vurdering.begrunnelse)
        assertThat(res.vurdering?.dokumenterBruktIVurdering).isEqualTo(vurdering.dokumenterBruktIVurdering)
        assertThat(res.vurdering?.harRettPå).isEqualTo(vurdering.harRettPå)
        assertThat(res.vurdering?.grunn).isEqualTo(vurdering.grunn)
        assertThat(res.vurdering?.vurdertAv).isEqualTo(vurdering.vurdertAv)
        assertThat(res.vurdering?.vurdertTidspunkt).isNotNull()

    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    }
}