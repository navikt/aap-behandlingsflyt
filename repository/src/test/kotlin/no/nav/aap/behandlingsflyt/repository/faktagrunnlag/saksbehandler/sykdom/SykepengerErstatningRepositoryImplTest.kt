package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class SykepengerErstatningRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }


    @Test
    fun `lagre og hente ut igjen`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }
        val vurdering1 = SykepengerVurdering(
            begrunnelse = "yolo",
            dokumenterBruktIVurdering = listOf(JournalpostId("123"), JournalpostId("321")),
            harRettPå = true,
            grunn = null,
            vurdertAv = "saksbehandler",
            gjelderFra = null,
            vurdertIBehandling = behandling.id
        )

        val vurdering2 = SykepengerVurdering(
            begrunnelse = "yolo x2",
            dokumenterBruktIVurdering = listOf(JournalpostId("456")),
            harRettPå = false,
            grunn = SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR,
            vurdertAv = "saksbehandler!!",
            vurdertIBehandling = behandling.id
        )

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling.id, listOf(vurdering1, vurdering2))
        }

        val res = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }!!

        assertThat(res.vurderinger)
            .usingRecursiveComparison()
            .ignoringFields("vurdertTidspunkt")
            .isEqualTo(listOf(vurdering1, vurdering2))

        assertThat(res.vurderinger).allSatisfy { vurdering ->
            assertThat(vurdering.vurdertTidspunkt).isNotNull
        }

        // Test sletting
        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).slett(behandling.id)
        }

        val res3 = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(res3).isNull()
    }
}
