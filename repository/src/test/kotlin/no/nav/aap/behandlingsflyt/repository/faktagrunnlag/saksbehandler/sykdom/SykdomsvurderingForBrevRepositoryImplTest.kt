package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class SykdomsvurderingForBrevRepositoryImplTest {
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
    fun `skal lagre og hente ut vurdering`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }

        val vurdering = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "en vurdering",
            vurdertAv = Bruker("saksbehandler"),
            vurdertTidspunkt = LocalDateTime.now(),
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, vurdering)
        }

        val lagretVurdering = dataSource.transaction {
            SykdomsvurderingForBrevRepositoryImpl(it).hent(behandling.id)
        }

        assertThat(lagretVurdering?.vurdering).isEqualTo(vurdering.vurdering)
        assertThat(lagretVurdering?.vurdertAv).isEqualTo(vurdering.vurdertAv)
        assertThat(lagretVurdering?.behandlingId).isEqualTo(vurdering.behandlingId)
        assertThat(lagretVurdering?.vurdertTidspunkt).isNotNull
        assertThat(lagretVurdering?.kilder).isEmpty()

        val oppdatertVurdering = vurdering.copy(
            vurdering = "oppdatert vurdering",
            vurdertAv = Bruker("annen saksbehandler")
        )

        val oppdatertLagretVurdering = dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, oppdatertVurdering)
            SykdomsvurderingForBrevRepositoryImpl(connection).hent(behandling.id)!!
        }

        assertThat(oppdatertLagretVurdering.vurdering).isEqualTo(oppdatertVurdering.vurdering)
        assertThat(oppdatertLagretVurdering.vurdertAv).isEqualTo(oppdatertVurdering.vurdertAv)
    }

    @Test
    fun `skal oppdatere vurdering`() {
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak(connection))
        }

        val vurdering = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "en vurdering",
            vurdertAv = Bruker("saksbehandler"),
            vurdertTidspunkt = LocalDateTime.now(),
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, vurdering)
        }

        val oppdatertVurdering = vurdering.copy(
            vurdering = "oppdatert vurdering",
            vurdertAv = Bruker("annen saksbehandler"),
            vurdertTidspunkt = LocalDateTime.now(),
        )

        val oppdatertLagretVurdering = dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, oppdatertVurdering)
            SykdomsvurderingForBrevRepositoryImpl(connection).hent(behandling.id)!!
        }

        assertThat(oppdatertLagretVurdering.vurdering).isEqualTo(oppdatertVurdering.vurdering)
        assertThat(oppdatertLagretVurdering.vurdertAv).isEqualTo(oppdatertVurdering.vurdertAv)
    }

    @Test
    fun `skal hente ut vurderinger for sak`() {
        val sak = dataSource.transaction { connection -> sak(connection) }
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak)
        }

        val vurdering = SykdomsvurderingForBrev(
            behandlingId = behandling.id,
            vurdering = "en vurdering",
            vurdertAv = Bruker("saksbehandler"),
            kilder = listOf(kilde("2"), kilde("1")),
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(behandling.id, vurdering)
        }

        val revurdering = dataSource.transaction { connection ->
            revurdering(connection, behandling)
        }

        val nyVurdering = SykdomsvurderingForBrev(
            behandlingId = revurdering.id,
            vurdering = "en ny vurdering",
            vurdertAv = Bruker("saksbehandler"),
            kilder = listOf(kilde("3")),
        )

        dataSource.transaction { connection ->
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(revurdering.id, nyVurdering)
            SykdomsvurderingForBrevRepositoryImpl(connection).lagre(
                behandling.id,
                vurdering.copy(kilder = listOf(kilde("4"), kilde("1")))
            )
        }

        val vurderingerForSak = dataSource.transaction {
            SykdomsvurderingForBrevRepositoryImpl(it).hent(sak.id)
        }

        assertThat(vurderingerForSak).size().isEqualTo(2)
        assertThat(vurderingerForSak.single { it.behandlingId == behandling.id }.kilder)
            .containsExactly(kilde("4"), kilde("1"))
        assertThat(vurderingerForSak.single { it.behandlingId == revurdering.id }.kilder)
            .containsExactlyElementsOf(nyVurdering.kilder)
    }

    @Test
    fun `skal lagre alle metadata og beholde kilderekkefolge for dokumenter i samme journalpost`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repository = SykdomsvurderingForBrevRepositoryImpl(connection)
            val kilder = listOf(
                kilde("3"),
                kilde("1").copy(
                    dokumentnavn = "Vedlegg med æøå",
                    journalføringsdato = LocalDate.of(2024, 2, 29)
                ),
                kilde("2"),
            )
            val vurdering = SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "Vurdering med kilder",
                vurdertAv = Bruker("saksbehandler"),
                kilder = kilder,
            )

            repository.lagre(behandling.id, vurdering)

            assertThat(repository.hent(behandling.id)?.kilder).containsExactlyElementsOf(kilder)
            assertThat(repository.hent(sak.id).single().kilder).containsExactlyElementsOf(kilder)

            repository.lagre(behandling.id, vurdering.copy(kilder = kilder.reversed()))

            assertThat(repository.hent(behandling.id)?.kilder).containsExactlyElementsOf(kilder.reversed())
            assertThat(repository.hent(sak.id).single().kilder).containsExactlyElementsOf(kilder.reversed())
        }
    }

    @Test
    fun `skal bevare kilder per versjon og tillate at alle kilder fjernes`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = SykdomsvurderingForBrevRepositoryImpl(connection)
            val tidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0)
            val opprinnelig = SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "Opprinnelig vurdering",
                vurdertAv = Bruker("saksbehandler"),
                vurdertTidspunkt = tidspunkt,
                kilder = listOf(kilde("2"), kilde("1")),
            )
            val oppdatert = opprinnelig.copy(
                vurdering = "Oppdatert vurdering",
                vurdertTidspunkt = tidspunkt.plusDays(1),
                kilder = listOf(kilde("1").copy(dokumentnavn = "Endret dokumentnavn"), kilde("3")),
            )
            repository.lagre(behandling.id, opprinnelig)
            repository.lagre(behandling.id, oppdatert)

            assertThat(repository.hent(behandling.id)).isEqualTo(oppdatert)
            assertThat(repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, tidspunkt.minusSeconds(1)))
                .isNull()
            assertThat(repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, tidspunkt.plusHours(1)))
                .isEqualTo(opprinnelig)
            assertThat(repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, tidspunkt.plusDays(1)))
                .isEqualTo(oppdatert)

            val utenKilder = oppdatert.copy(vurdertTidspunkt = tidspunkt.plusDays(2), kilder = emptyList())
            repository.lagre(behandling.id, utenKilder)

            assertThat(repository.hent(behandling.id)).isEqualTo(utenKilder)
            assertThat(repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, tidspunkt.plusDays(1)))
                .isEqualTo(oppdatert)
            assertThat(repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, tidspunkt))
                .isEqualTo(opprinnelig)
        }
    }

    @Test
    fun `skal hente nyeste kildevalg ved like vurderingstidspunkter`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = SykdomsvurderingForBrevRepositoryImpl(connection)
            val opprinnelig = SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "Vurdering",
                vurdertAv = Bruker("saksbehandler"),
                vurdertTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0),
                kilder = listOf(kilde("1")),
            )
            val oppdatert = opprinnelig.copy(kilder = listOf(kilde("2")))
            repository.lagre(behandling.id, opprinnelig)
            repository.lagre(behandling.id, oppdatert)

            assertThat(
                repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, opprinnelig.vurdertTidspunkt)
            ).isEqualTo(oppdatert)
        }
    }

    @Test
    fun `skal hente eldre vurdering uten kilder selv om nyere vurdering har kilder`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = SykdomsvurderingForBrevRepositoryImpl(connection)
            val opprinnelig = SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "Eldre vurdering uten kilder",
                vurdertAv = Bruker("saksbehandler"),
                vurdertTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0),
            )
            repository.lagre(behandling.id, opprinnelig)
            repository.lagre(
                behandling.id,
                opprinnelig.copy(
                    vurdertTidspunkt = opprinnelig.vurdertTidspunkt.plusDays(1),
                    kilder = listOf(kilde("1")),
                )
            )

            assertThat(
                repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, opprinnelig.vurdertTidspunkt)
            ).isEqualTo(opprinnelig)
        }
    }

    @Test
    fun `skal beholde historiske kilder etter deaktivering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val repository = SykdomsvurderingForBrevRepositoryImpl(connection)
            val vurdering = SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "Vurdering med kilder",
                vurdertAv = Bruker("saksbehandler"),
                vurdertTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0),
                kilder = listOf(kilde("2"), kilde("1")),
            )
            repository.lagre(behandling.id, vurdering)

            repository.deaktiverEksisterende(behandling.id)

            assertThat(repository.hent(behandling.id)).isNull()
            assertThat(repository.hent(sak.id)).isEmpty()
            assertThat(
                repository.hentSykdomsvurderingForBrevPåTidspunkt(behandling.id, vurdering.vurdertTidspunkt)
            ).isEqualTo(vurdering)
        }
    }

    @Test
    fun `skal slette kilder for alle versjoner ved sletting av vurdering`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = SykdomsvurderingForBrevRepositoryImpl(connection)
            val vurdering = SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "Vurdering med kilder",
                vurdertAv = Bruker("saksbehandler"),
                vurdertTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0),
                kilder = listOf(kilde("2"), kilde("1")),
            )
            repository.lagre(behandling.id, vurdering)
            repository.lagre(
                behandling.id,
                vurdering.copy(
                    vurdertTidspunkt = vurdering.vurdertTidspunkt.plusDays(1),
                    kilder = listOf(kilde("3")),
                )
            )
            val vurderingIder = connection.queryList(
                "SELECT id FROM SYKDOM_VURDERING_BREV WHERE behandling_id = ?"
            ) {
                setParams { setLong(1, behandling.id.toLong()) }
                setRowMapper { it.getLong("id") }
            }
            assertThat(vurderingIder).hasSize(2)

            repository.slett(behandling.id)

            assertThat(repository.hent(behandling.id)).isNull()
            assertThat(
                repository.hentSykdomsvurderingForBrevPåTidspunkt(
                    behandling.id, vurdering.vurdertTidspunkt.plusDays(2)
                )
            ).isNull()
            val antallKilder = connection.queryFirst(
                """
                    SELECT count(*) AS antall FROM SYKDOM_VURDERING_BREV_KILDE
                    WHERE sykdom_vurdering_brev_id = ANY(?::bigint[])
                """.trimIndent()
            ) {
                setParams { setLongArray(1, vurderingIder) }
                setRowMapper { it.getLong("antall") }
            }
            assertThat(antallKilder).isZero()
        }
    }

    @Test
    fun `skal ikke kopiere vurdering eller kilder til revurdering`() {
        dataSource.transaction { connection ->
            val behandling = finnEllerOpprettBehandling(connection, sak(connection))
            val repository = SykdomsvurderingForBrevRepositoryImpl(connection)
            val vurdering = SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "Vurdering med kilder",
                vurdertAv = Bruker("saksbehandler"),
                kilder = listOf(kilde("2"), kilde("1")),
            )
            repository.lagre(behandling.id, vurdering)
            val revurdering = revurdering(connection, behandling)

            repository.kopier(behandling.id, revurdering.id)

            assertThat(repository.hent(revurdering.id)).isNull()
            assertThat(repository.hent(behandling.id)?.kilder).containsExactlyElementsOf(vurdering.kilder)
        }
    }

    private fun kilde(dokumentInfoId: String) = Dokumentkilde(
        journalpostId = "journalpost-1",
        dokumentInfoId = dokumentInfoId,
        dokumentnavn = "Dokument $dokumentInfoId",
        journalføringsdato = LocalDate.of(2025, 1, 1),
    )

    private fun revurdering(connection: DBConnection, behandling: Behandling): Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            behandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
    }
}