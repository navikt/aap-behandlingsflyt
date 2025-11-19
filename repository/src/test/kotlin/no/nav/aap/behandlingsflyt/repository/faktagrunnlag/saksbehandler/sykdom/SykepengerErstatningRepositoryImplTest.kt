package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

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

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    }

    /**
     * Midlertidige tester for å verifisere migrering, slettes sammen med at migreringskoden slettes
     */

    @Test
    fun `test at migrering funker for bare ett grunnlag med flere vurderinger`() {
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
            vurdertIBehandling = null
        )

        val vurdering2 = SykepengerVurdering(
            begrunnelse = "yolo x2",
            dokumenterBruktIVurdering = listOf(JournalpostId("456")),
            harRettPå = false,
            grunn = SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR,
            vurdertAv = "saksbehandler!!",
            vurdertIBehandling = null
        )

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling.id, listOf(vurdering1, vurdering2))
        }

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).migrerPeriodisering()
        }

        val res = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }!!

        assertThat(res.vurderinger).allSatisfy { it.vurdertIBehandling == behandling.id }
    }

    @Test
    fun `Skal lagre forrige vedtatte + nye vurderinger`() {
        val (sak, behandling1) = dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            Pair(sak, behandling)
        }

        val vurdering1 = SykepengerVurdering(
            begrunnelse = "yolo",
            dokumenterBruktIVurdering = listOf(JournalpostId("123"), JournalpostId("321")),
            harRettPå = true,
            grunn = null,
            vurdertAv = "saksbehandler",
            gjelderFra = null,
            vurdertIBehandling = behandling1.id
        )

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling1.id, listOf(vurdering1))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
        }

        val behandling2 = dataSource.transaction { connection ->
            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            val res = SykepengerErstatningRepositoryImpl(connection).hentHvisEksisterer(behandling2.id)
            assertThat(res!!.vurderinger).usingRecursiveComparison()
                .ignoringFields("vurdertTidspunkt")
                .isEqualTo(listOf(vurdering1))
            behandling2
        }

        val vurdering2 = SykepengerVurdering(
            begrunnelse = "yolo x2",
            dokumenterBruktIVurdering = listOf(JournalpostId("456")),
            harRettPå = false,
            grunn = SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR,
            vurdertAv = "saksbehandler!!",
            vurdertIBehandling = behandling2.id
        )

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling2.id, listOf(vurdering2))
        }

        val vurdering3 = SykepengerVurdering(
            begrunnelse = "Ny vurdering",
            dokumenterBruktIVurdering = listOf(JournalpostId("456")),
            harRettPå = true,
            grunn = SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR,
            vurdertAv = "saksbehandler!!",
            vurdertIBehandling = behandling2.id
        )
        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling2.id, listOf(vurdering3))
        }

        val res = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }!!

        assertThat(res.vurderinger)
            .usingRecursiveComparison()
            .ignoringFields("vurdertTidspunkt")
            .isEqualTo(listOf(vurdering1, vurdering3))

        assertThat(res.vurderinger).allSatisfy { vurdering ->
            assertThat(vurdering.vurdertTidspunkt).isNotNull
        }
    }

    @Test
    fun `test at migrering funker for flere behandlinger på samme sak `() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }

        val behandling2 = dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            behandlingRepository.opprettBehandling(
                behandling.sakId,
                TypeBehandling.Revurdering,
                forrigeBehandlingId = behandling.id,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    årsak = ÅrsakTilOpprettelse.SØKNAD,
                )
            )
        }

        val vurdering1 = SykepengerVurdering(
            begrunnelse = "yolo",
            dokumenterBruktIVurdering = listOf(JournalpostId("123"), JournalpostId("321")),
            harRettPå = true,
            grunn = null,
            vurdertAv = "saksbehandler",
            gjelderFra = null,
            vurdertIBehandling = null
        )

        val vurdering2 = SykepengerVurdering(
            begrunnelse = "yolo x2",
            dokumenterBruktIVurdering = listOf(JournalpostId("456")),
            harRettPå = false,
            grunn = SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR,
            vurdertAv = "saksbehandler!!",
            vurdertIBehandling = null
        )

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling.id, listOf(vurdering1))
            SykepengerErstatningRepositoryImpl(connection).lagre(behandling2.id, listOf(vurdering1, vurdering2))
        }

        dataSource.transaction { connection ->
            SykepengerErstatningRepositoryImpl(connection).migrerPeriodisering()
        }

        val res1 = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }!!

        val res2 = dataSource.transaction {
            SykepengerErstatningRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }!!

        assertThat(res1.vurderinger).hasSize(1)
        assertThat(res1.vurderinger).allSatisfy { it.vurdertIBehandling == behandling.id }

        assertThat(res2.vurderinger).hasSize(2)
        assertThat(res2.vurderinger).anyMatch { it.vurdertIBehandling == behandling.id }
        assertThat(res2.vurderinger).anyMatch { it.vurdertIBehandling == behandling2.id }
    }
}
