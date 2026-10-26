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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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

    @ParameterizedTest
    @EnumSource(EtableringRegelsett::class)
    fun `lagrer og henter vurderinger riktig`(regelsett: EtableringRegelsett) {
        dataSource.transaction { connection ->
            val repository = EtableringEgenVirksomhetRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurdering =
                EtableringEgenVirksomhetVurdering(
                    begrunnelse = "test",
                    fom = LocalDate.now(),
                    tom = null,
                    vurdertAv = Bruker("1234"),
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id,
                    virksomhetNavn = "Kattepensjonatet",
                    orgNr = "12344311234",
                    foreliggerFagligVurdering = true,
                    virksomhetErNy = true,
                    brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                    kanFøreTilSelvforsørget = true,
                    utviklingsPerioder = emptyList(),
                    oppstartsPerioder = emptyList(),
                    fase = null,
                    erRegistrertINødvendigeOffentligeRegister = null,
                    jobberBrukerAktivMedVirksomheten = null,
                    regelsett = regelsett,
                )

            repository.lagre(behandling.id, listOf(vurdering))

            val lagredeRegelsett = connection.queryList(
                "SELECT regelsett FROM etablering_egen_virksomhet_vurdering WHERE vurdert_i_behandling = ?"
            ) {
                setParams { setLong(1, behandling.id.id) }
                setRowMapper { it.getString("regelsett") }
            }
            assertThat(lagredeRegelsett).containsExactly(regelsett.name)

            val actual = repository.hentHvisEksisterer(behandling.id)
            assertThat(actual?.vurderinger)
                .usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(vurdering))

            val nesteBehandling = finnEllerOpprettBehandling(connection, sak(connection))
            repository.kopier(behandling.id, nesteBehandling.id)
            assertThat(repository.hentHvisEksisterer(nesteBehandling.id)?.vurderinger)
                .usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(vurdering))

            connection.execute(
                "UPDATE etablering_egen_virksomhet_vurdering SET regelsett = DEFAULT WHERE vurdert_i_behandling = ?"
            ) {
                setParams { setLong(1, behandling.id.id) }
            }
            assertThat(repository.hentHvisEksisterer(behandling.id)?.vurderinger?.single()?.regelsett)
                .isEqualTo(EtableringRegelsett.LEGACY)
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
                        fom = LocalDate.now(),
                        tom = null,
                        vurdertAv = Bruker("1234"),
                        opprettet = Instant.now(),
                        vurdertIBehandling = behandling.id,
                        virksomhetNavn = "Kattepensjonatet",
                        orgNr = "12344311234",
                        foreliggerFagligVurdering = true,
                        virksomhetErNy = true,
                        brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
                        kanFøreTilSelvforsørget = true,
                        fase = EtableringFase.OPPSTART,
                        erRegistrertINødvendigeOffentligeRegister = true,
                        jobberBrukerAktivMedVirksomheten = true
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