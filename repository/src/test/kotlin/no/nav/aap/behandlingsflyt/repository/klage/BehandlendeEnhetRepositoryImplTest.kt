package no.nav.aap.behandlingsflyt.repository.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage.BehandlendeEnhetRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

internal class BehandlendeEnhetRepositoryImplTest {
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
    fun `Lagrer og henter påklagetbehandling`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            val klageBehandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)

            val behandlendeEnhetRepository = BehandlendeEnhetRepositoryImpl(connection)
            val vurdering = BehandlendeEnhetVurdering(
                skalBehandlesAvNay = true,
                skalBehandlesAvKontor = false,
                vurdertAv = "ident"
            )

            behandlendeEnhetRepository.lagre(klageBehandling.id, vurdering)
            val grunnlag = behandlendeEnhetRepository.hentHvisEksisterer(klageBehandling.id)!!
            assertThat(grunnlag.vurdering.skalBehandlesAvNay).isTrue()
            assertThat(grunnlag.vurdering.skalBehandlesAvKontor).isFalse()
            assertThat(grunnlag.vurdering.vurdertAv).isEqualTo("ident")
            assertNotNull(grunnlag.vurdering.opprettet)
        }
    }
}