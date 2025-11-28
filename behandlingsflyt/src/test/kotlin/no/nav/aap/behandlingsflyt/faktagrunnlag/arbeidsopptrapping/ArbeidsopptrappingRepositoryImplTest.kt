package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
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
    fun `hentPerioder filtrerer ut vurderinger som ikke har rettigheter`() {
        dataSource.transaction { connection ->
            val repository = ArbeidsopptrappingRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repository.lagre(
                behandling.id,
                listOf(
                    ArbeidsopptrappingVurdering(
                        begrunnelse = "test1",
                        vurderingenGjelderFra = LocalDate.of(2024, 1, 1),
                        vurderingenGjelderTil = null,
                        reellMulighetTilOpptrapping = false,
                        rettPaaAAPIOpptrapping = true,
                        vurdertAv = "aa",
                        opprettetTid = Instant.now(),
                        vurdertIBehandling = behandling.id
                    ),
                    ArbeidsopptrappingVurdering(
                        begrunnelse = "test2",
                        vurderingenGjelderFra = LocalDate.of(2024, 2, 1),
                        vurderingenGjelderTil = null,
                        reellMulighetTilOpptrapping = true,
                        rettPaaAAPIOpptrapping = true,
                        vurdertAv = "bb",
                        opprettetTid = Instant.now(),
                        vurdertIBehandling = behandling.id
                    )
                )
            )

            val perioder = repository.hentPerioder(behandling.id)

            assertThat(perioder).hasSize(1)
            assertThat(perioder.first().fom).isEqualTo(LocalDate.of(2024, 2, 1))
        }
    }

    @Test
    fun `hentPerioder bruker neste vurdering som tom-dato hvis vurderingenGjelderTil er null`() {
        dataSource.transaction { connection ->
            val repository = ArbeidsopptrappingRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repository.lagre(
                behandling.id,
                listOf(
                    ArbeidsopptrappingVurdering(
                        begrunnelse = "test",
                        vurderingenGjelderFra = LocalDate.of(2024, 1, 1),
                        vurderingenGjelderTil = null,
                        reellMulighetTilOpptrapping = true,
                        rettPaaAAPIOpptrapping = true,
                        vurdertAv = "aa",
                        opprettetTid = Instant.now(),
                        vurdertIBehandling = behandling.id
                    ),
                    ArbeidsopptrappingVurdering(
                        begrunnelse = "test",
                        vurderingenGjelderFra = LocalDate.of(2024, 3, 1),
                        vurderingenGjelderTil = null,
                        reellMulighetTilOpptrapping = true,
                        rettPaaAAPIOpptrapping = true,
                        vurdertAv = "bb",
                        opprettetTid = Instant.now(),
                        vurdertIBehandling = behandling.id
                    )
                )
            )

            val perioder = repository.hentPerioder(behandling.id)

            assertThat(perioder).containsExactly(
                Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1)),
                Periode(LocalDate.of(2024, 3, 1), LocalDate.of(2025, 3, 1))
            )
        }
    }

    @Test
    fun `hentPerioder gir siste vurdering 12 måneder varighet hvis ingen neste og ingen til-dato`() {
        dataSource.transaction { connection ->
            val repository = ArbeidsopptrappingRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            repository.lagre(
                behandling.id,
                listOf(
                    ArbeidsopptrappingVurdering(
                        begrunnelse = "test",
                        vurderingenGjelderFra = LocalDate.of(2024, 6, 1),
                        vurderingenGjelderTil = null,
                        reellMulighetTilOpptrapping = true,
                        rettPaaAAPIOpptrapping = true,
                        vurdertAv = "aa",
                        opprettetTid = Instant.now(),
                        vurdertIBehandling = behandling.id
                    )
                )
            )

            val perioder = repository.hentPerioder(behandling.id)

            assertThat(perioder.single())
                .isEqualTo(Periode(LocalDate.of(2024, 6, 1), LocalDate.of(2025, 6, 1)))
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
        }
    }
}