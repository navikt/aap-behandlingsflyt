package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.time.LocalDate

class EtableringEgenVirksomhetRepositoryTest {
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
            val repository = EtableringEgenVirksomhetRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurdering =
                EtableringEgenVirksomhetVurdering(
                    begrunnelse = "test",
                    vurderingenGjelderFra = LocalDate.now(),
                    vurderingenGjelderTil = null,
                    vurdertAv = Bruker("1234"),
                    opprettetTid = Instant.now(),
                    vurdertIBehandling = behandling.id,
                    virksomhetNavn = "Kattepensjonatet",
                    orgNr = "12344311234",
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    utviklingsPerioder = emptyList(),
                    oppstartsPerioder = emptyList(),
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
            val repository = EtableringEgenVirksomhetRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repository.lagre(
                behandling.id,
                listOf(
                    EtableringEgenVirksomhetVurdering(
                        begrunnelse = "test",
                        vurderingenGjelderFra = LocalDate.now(),
                        vurderingenGjelderTil = null,
                        vurdertAv = Bruker("1234"),
                        opprettetTid = Instant.now(),
                        vurdertIBehandling = behandling.id,
                        virksomhetNavn = "Kattepensjonatet",
                        orgNr = "12344311234",
                        foreliggerFagligVurdering = true,
                        virksomhetErNy = true,
                        brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                        kanFøreTilSelvforsørget = true,
                        utviklingsPerioder = listOf(Periode(LocalDate.now(), LocalDate.now().plusMonths(5))),
                        oppstartsPerioder = listOf(Periode(LocalDate.now().plusMonths(6), LocalDate.now().plusMonths(9))),
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