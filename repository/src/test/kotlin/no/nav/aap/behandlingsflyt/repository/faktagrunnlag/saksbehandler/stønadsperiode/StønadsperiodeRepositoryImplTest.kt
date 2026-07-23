package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*

class StønadsperiodeRepositoryImplTest {

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

    private fun opprettBehandlingId(): BehandlingId = dataSource.transaction { connection ->
        val sak = opprettSak(connection, 1 januar 2026)
        finnEllerOpprettBehandling(connection, sak).id
    }

    private fun vurdering(
        behandlingId: BehandlingId,
        referanse: Kravreferanse = Kravreferanse(UUID.randomUUID()),
        relevantKravType: RelevantKravType = RelevantKravType.AVSLAG,
        startDato: LocalDate = LocalDate.now(),
    ) = StønadsperiodeVurdering(
        referanse = referanse,
        begrunnelse = "begrunnelse",
        harHattOrdinærSiste52Uker = false,
        harGjenværendeKvote = false,
        relevantKravType = relevantKravType,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        vurdertAv = SYSTEMBRUKER,
        startDato = startDato
    )

    @Test
    fun `hentHvisEksisterer returnerer null når ingen grunnlag er lagret`() {
        val behandlingId = opprettBehandlingId()

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNull()
    }

    @Test
    fun `lagre og hent én vurdering`() {
        val behandlingId = opprettBehandlingId()
        val ref = Kravreferanse(UUID.randomUUID())

        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId, ref))
            )
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNotNull
        assertThat(grunnlag!!.vurderinger).hasSize(1)
        assertThat(grunnlag.vurderinger.first().referanse).isEqualTo(ref)
        assertThat(grunnlag.vurderinger.first().relevantKravType).isEqualTo(RelevantKravType.AVSLAG)
    }

    @Test
    fun `lagre og hent flere vurderinger`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).lagre(
                behandlingId, setOf(
                    vurdering(behandlingId),
                    vurdering(behandlingId),
                )
            )
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }!!

        assertThat(grunnlag.vurderinger).hasSize(2)
    }

    @Test
    fun `lagre deaktiverer gammelt grunnlag og lagrer nytt`() {
        val behandlingId = opprettBehandlingId()
        val ref = Kravreferanse(UUID.randomUUID())

        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId, ref, RelevantKravType.AVSLAG))
            )
        }
        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId, ref, RelevantKravType.NY_STØNADSPERIODE))
            )
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }!!

        assertThat(grunnlag.vurderinger).hasSize(1)
        assertThat(grunnlag.vurderinger.first().relevantKravType).isEqualTo(RelevantKravType.NY_STØNADSPERIODE)
    }

    @Test
    fun `kopier kopierer grunnlag til ny behandling`() {
        val fraBehandlingId = opprettBehandlingId()
        val tilBehandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).lagre(
                fraBehandlingId, setOf(vurdering(fraBehandlingId))
            )
        }
        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).kopier(fraBehandlingId, tilBehandlingId)
        }

        val kopiert = dataSource.transaction(readOnly = true) { connection ->
            StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(tilBehandlingId)
        }

        assertThat(kopiert).isNotNull
        assertThat(kopiert!!.vurderinger).hasSize(1)
    }

    @Test
    fun `tilbakestillGrunnlag uten forrigeBehandling fjerner grunnlag`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId))
            )
        }
        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).tilbakestillGrunnlag(behandlingId, null)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNull()
    }

    @Test
    fun `tilbakestillGrunnlag med forrigeBehandling kopierer fra forrige`() {
        val forrigeBehandlingId = opprettBehandlingId()
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).lagre(
                forrigeBehandlingId, setOf(vurdering(forrigeBehandlingId))
            )
        }
        dataSource.transaction { connection ->
            StønadsperiodeRepositoryImpl(connection).tilbakestillGrunnlag(behandlingId, forrigeBehandlingId)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNotNull
    }

    @Test
    fun `vurdering med alle relevantKravType-verdier lagres og hentes korrekt`() {
        RelevantKravType.entries.forEach { kravType ->
            val behandlingId = opprettBehandlingId()
            val harGjenværende = kravType == RelevantKravType.GJENOPPTAK_ETTER_STANS ||
                    kravType == RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR
            val vurdering = StønadsperiodeVurdering(
                referanse = Kravreferanse(UUID.randomUUID()),
                begrunnelse = "test $kravType",
                harHattOrdinærSiste52Uker = harGjenværende,
                harGjenværendeKvote = harGjenværende,
                relevantKravType = kravType,
                vurdertIBehandling = behandlingId,
                opprettet = Instant.now(),
                vurdertAv = SYSTEMBRUKER,
                startDato = LocalDate.now()
            )

            dataSource.transaction { connection ->
                StønadsperiodeRepositoryImpl(connection).lagre(behandlingId, setOf(vurdering))
            }

            val lagret = dataSource.transaction(readOnly = true) { connection ->
                StønadsperiodeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
            }!!.vurderinger.first()

            assertThat(lagret.relevantKravType).isEqualTo(kravType)
            assertThat(lagret.harHattOrdinærSiste52Uker).isEqualTo(harGjenværende)
            assertThat(lagret.harGjenværendeKvote).isEqualTo(harGjenværende)
        }
    }
}
