package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.inntektsbortfall

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class InntektsbortfallRepositoryImplTest {
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
    fun `lagre og hente inntektsbortfallvurdering, og slett`() {
        val sak = dataSource.transaction { sak(it) }
        val behandling = dataSource.transaction { connection ->
            finnEllerOpprettBehandling(connection, sak)
        }

        val vurdering = InntektsbortfallVurdering(
            begrunnelse = "En begrunnelse",
            rettTilUttak = true,
            vurdertAv = "Z123456",
            vurdertIBehandling = behandling.id
        )

        dataSource.transaction { connection ->
            val repository = InntektsbortfallRepositoryImpl(connection)
            repository.lagre(behandling.id, vurdering)
        }

        dataSource.transaction { connection ->
            val repository = InntektsbortfallRepositoryImpl(connection)
            val hentet = repository.hentHvisEksisterer(behandling.id)

            assertThat(hentet).isEqualTo(vurdering)
        }

        dataSource.transaction { connection ->
            val repository = InntektsbortfallRepositoryImpl(connection)
            repository.slett(behandling.id)
        }

        dataSource.transaction { connection ->
            val repository = InntektsbortfallRepositoryImpl(connection)
            assertThat(repository.hentHvisEksisterer(behandling.id)).isNull()
        }
    }
}
