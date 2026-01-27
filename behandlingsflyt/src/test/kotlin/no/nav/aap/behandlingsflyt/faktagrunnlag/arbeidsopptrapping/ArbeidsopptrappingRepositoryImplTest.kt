package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.time.LocalDate

internal class ArbeidsopptrappingRepositoryImplTest {

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
    fun `lagrer og henter vurderinger riktig`() {
        dataSource.transaction { connection ->
            val repository = ArbeidsopptrappingRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurdering = ArbeidsopptrappingVurdering(
                begrunnelse = "test",
                vurderingenGjelderFra = LocalDate.of(2025, 1, 1),
                vurderingenGjelderTil = LocalDate.of(2025, 1, 31),
                reellMulighetTilOpptrapping = true,
                rettPaaAAPIOpptrapping = true,
                vurdertAv = "Saksbehandler",
                opprettetTid = Instant.now(),
                vurdertIBehandling = behandling.id
            )

            repository.lagre(behandling.id, listOf(vurdering))

            val actual = repository.hentHvisEksisterer(behandling.id)
            assertThat(actual?.vurderinger)
                .usingRecursiveComparison()
                .ignoringFields("opprettetTid")
                .isEqualTo(listOf(vurdering))
        }
    }

    @Test
    fun `kan slette vurderinger`() {
        dataSource.transaction { connection ->
            val repository = ArbeidsopptrappingRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repository.lagre(
                behandling.id,
                listOf(
                    ArbeidsopptrappingVurdering(
                        begrunnelse = "test",
                        vurderingenGjelderFra = LocalDate.now(),
                        vurderingenGjelderTil = null,
                        reellMulighetTilOpptrapping = true,
                        rettPaaAAPIOpptrapping = true,
                        vurdertAv = "aa",
                        opprettetTid = Instant.now(),
                        vurdertIBehandling = behandling.id
                    )
                )
            )

            assertDoesNotThrow {
                repository.slett(behandling.id)
            }

            assertThat(repository.hentHvisEksisterer(behandling.id)).isNull()
        }
    }
}