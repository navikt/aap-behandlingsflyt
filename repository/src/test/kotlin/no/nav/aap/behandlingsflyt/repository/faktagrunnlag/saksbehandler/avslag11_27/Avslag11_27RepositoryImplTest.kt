package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avslag11_27

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class Avslag11_27RepositoryImplTest {

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
        skalAvslås: Boolean = true,
    ) = Avslag11_27Vurdering(
        referanse = referanse,
        begrunnelse = "begrunnelse",
        harAnnenFullYtelse = skalAvslås,
        brukersYtelse = if (skalAvslås) Ytelse.SYKEPENGER else null,
        harSykepengegrunnlagOver2G = null,
        skalAvslås1127 = skalAvslås,
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        vurdertAv = Bruker("test"),
    )

    @Test
    fun `hentHvisEksisterer returnerer null når ingen grunnlag er lagret`() {
        val behandlingId = opprettBehandlingId()

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNull()
    }

    @Test
    fun `lagre og hent én vurdering`() {
        val behandlingId = opprettBehandlingId()
        val ref = Kravreferanse(UUID.randomUUID())

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId, ref, skalAvslås = true))
            )
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNotNull
        assertThat(grunnlag!!.vurderinger).hasSize(1)
        assertThat(grunnlag.vurderinger.first().referanse).isEqualTo(ref)
        assertThat(grunnlag.vurderinger.first().skalAvslås1127).isTrue()
        assertThat(grunnlag.vurderinger.first().harAnnenFullYtelse).isTrue()
        assertThat(grunnlag.vurderinger.first().brukersYtelse).isEqualTo(Ytelse.SYKEPENGER)
    }

    @Test
    fun `lagre og hent flere vurderinger`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(
                behandlingId, setOf(
                    vurdering(behandlingId, skalAvslås = true),
                    vurdering(behandlingId, skalAvslås = false),
                )
            )
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }!!

        assertThat(grunnlag.vurderinger).hasSize(2)
    }

    @Test
    fun `lagre deaktiverer gammelt grunnlag og lagrer nytt`() {
        val behandlingId = opprettBehandlingId()
        val ref = Kravreferanse(UUID.randomUUID())

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId, ref, skalAvslås = true))
            )
        }
        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId, ref, skalAvslås = false))
            )
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }!!

        assertThat(grunnlag.vurderinger).hasSize(1)
        assertThat(grunnlag.vurderinger.first().skalAvslås1127).isFalse()
    }

    @Test
    fun `kopier kopierer grunnlag til ny behandling`() {
        val fraBehandlingId = opprettBehandlingId()
        val tilBehandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(
                fraBehandlingId, setOf(vurdering(fraBehandlingId, skalAvslås = true))
            )
        }
        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).kopier(fraBehandlingId, tilBehandlingId)
        }

        val kopiert = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(tilBehandlingId)
        }

        assertThat(kopiert).isNotNull
        assertThat(kopiert!!.vurderinger).hasSize(1)
    }

    @Test
    fun `tilbakestillGrunnlag uten forrigeBehandling fjerner grunnlag`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(
                behandlingId, setOf(vurdering(behandlingId))
            )
        }
        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).tilbakestillGrunnlag(behandlingId, null)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNull()
    }

    @Test
    fun `tilbakestillGrunnlag med forrigeBehandling kopierer fra forrige`() {
        val forrigeBehandlingId = opprettBehandlingId()
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(
                forrigeBehandlingId, setOf(vurdering(forrigeBehandlingId, skalAvslås = true))
            )
        }
        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).tilbakestillGrunnlag(behandlingId, forrigeBehandlingId)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNotNull
    }

    @Test
    fun `vurdering med harSykepengegrunnlagOver2G lagres og hentes`() {
        val behandlingId = opprettBehandlingId()
        val vurdering = Avslag11_27Vurdering(
            referanse = Kravreferanse(UUID.randomUUID()),
            begrunnelse = "sykepenger over 2G",
            harAnnenFullYtelse = true,
            brukersYtelse = Ytelse.SYKEPENGER,
            harSykepengegrunnlagOver2G = true,
            skalAvslås1127 = true,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            vurdertAv = Bruker("test"),
        )

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(behandlingId, setOf(vurdering))
        }

        val lagret = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }!!.vurderinger.first()

        assertThat(lagret.harSykepengegrunnlagOver2G).isTrue()
        assertThat(lagret.brukersYtelse).isEqualTo(Ytelse.SYKEPENGER)
    }

    @Test
    fun `lagre tom liste gir null ved hent`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            Avslag11_27RepositoryImpl(connection).lagre(behandlingId, emptySet())
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            Avslag11_27RepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNull()
    }
}