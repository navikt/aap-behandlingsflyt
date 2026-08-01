package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.sykdom.Diagnose
import no.nav.aap.sykdom.Sykdomsvurdering
import no.nav.aap.sykdom.YrkesskadeSak
import no.nav.aap.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class SykdomRepositoryImplTest {
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

        private fun sykdomsvurdering1(behandlingId: BehandlingId = BehandlingId(1L)) = Sykdomsvurdering(
            begrunnelse = "b1",
            vurderingenGjelderFra = 1 januar 2020,
            vurderingenGjelderTil = null,
            harSkadeSykdomEllerLyte = true,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            yrkesskadeBegrunnelse = "b",
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            diagnose = Diagnose("ICDP", "PEST", listOf("KOLERA", "ARGUSØYNE")),
            vurdertIBehandling = behandlingId,
        )

        private fun sykdomsvurdering2(
            behandlingId: BehandlingId = BehandlingId(1L),
            vurderingenGjelderFra: LocalDate = LocalDate.of(2020, 1, 1)
        ) = Sykdomsvurdering(
            begrunnelse = "b2",
            vurderingenGjelderFra = vurderingenGjelderFra,
            vurderingenGjelderTil = null,
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            yrkesskadeBegrunnelse = null,
            vurdertAv = Bruker("Z00000"),
            opprettet = Instant.now(),
            diagnose = null,
            vurdertIBehandling = behandlingId,
        )
    }

    @Test
    fun `kan lagre tom liste`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            sykdomRepo.lagre(behandling.id, emptyList())
            assertThat(sykdomRepo.hent(behandling.id).sykdomsvurderinger).isEmpty()
        }
    }

    @Test
    fun `kan lagre singleton-liste`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val sykdomsvurdering1 = sykdomsvurdering1(behandling.id)
            sykdomRepo.lagre(behandling.id, listOf(sykdomsvurdering1))
            val sykdomsvurderinger = sykdomRepo.hent(behandling.id).sykdomsvurderinger
            assertThat(sykdomsvurderinger).usingRecursiveComparison()
                .ignoringFields("id", "opprettet").isEqualTo(listOf(sykdomsvurdering1))
            assertThat(sykdomsvurderinger[0].opprettet)
                .isCloseTo(sykdomsvurdering1.opprettet, within(1, ChronoUnit.SECONDS))
        }
    }

    @Test
    fun `kan lagre to elementer`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val sykdomsvurdering1 = sykdomsvurdering1(behandling.id)
            val sykdomsvurdering2 = sykdomsvurdering2(behandling.id)

            sykdomRepo.lagre(behandling.id, listOf(sykdomsvurdering1, sykdomsvurdering2))
            assertThat(sykdomRepo.hent(behandling.id).sykdomsvurderinger).usingRecursiveComparison()
                .ignoringFields("id", "opprettet").isEqualTo(
                    listOf(
                        sykdomsvurdering1, sykdomsvurdering2
                    )
                )
        }
    }

    @Test
    fun `lagre og hente ned yrkesskade-vurdering`() {
        dataSource.transaction { connection ->
            val sykdomRepo = SykdomRepositoryImpl(connection)
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val vurdering = Yrkesskadevurdering(
                begrunnelse = "begr",
                relevanteSaker = listOf(
                    YrkesskadeSak(
                        referanse = "gokk", manuellYrkesskadeDato = LocalDate.now()
                    )
                ),
                erÅrsakssammenheng = true,
                andelAvNedsettelsen = Prosent(70),
                vurdertAv = Bruker("Grokki Grokk"),
                vurdertTidspunkt = LocalDateTime.now()
            )
            sykdomRepo.lagre(behandling.id, vurdering)
            assertThat(sykdomRepo.hent(behandling.id).yrkesskadevurdering).usingRecursiveComparison()
                .ignoringFields("id", "vurdertTidspunkt").isEqualTo(vurdering)
        }
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val sykdomRepository = SykdomRepositoryImpl(connection)
                sykdomRepository.lagre(
                    behandling.id, listOf(
                        Sykdomsvurdering(
                            begrunnelse = "b1",
                            vurderingenGjelderFra = 1 januar 2020,
                            vurderingenGjelderTil = null,
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            yrkesskadeBegrunnelse = "b",
                            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                            vurdertAv = Bruker("Z00000"),
                            vurdertIBehandling = behandling.id,
                            diagnose = null,
                            opprettet = Instant.now(),
                        )
                    )
                )
                assertDoesNotThrow {
                    sykdomRepository.slett(behandling.id)
                }
            }
        }
    }

}
