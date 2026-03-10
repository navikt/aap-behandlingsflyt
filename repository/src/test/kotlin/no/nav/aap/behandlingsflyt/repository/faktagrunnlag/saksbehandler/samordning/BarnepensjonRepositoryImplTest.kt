package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonPeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.YearMonth

class BarnepensjonRepositoryImplTest {
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

    private val sammenligner: RecursiveComparisonConfiguration =
        RecursiveComparisonConfiguration.builder().withIgnoredFields("opprettet", "id").build()

    @Test
    fun `lagre, hente ut, slette`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val vurdering = BarnepensjonVurdering(
            begrunnelse = "begrunnelse",
            perioder = setOf(
                BarnepensjonPeriode(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2022, 12),
                    månedsats = Beløp(10000)
                ),
                BarnepensjonPeriode(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 12),
                    månedsats = Beløp(25000)
                )
            ),
            vurdertAv = Bruker("Z123456"),
            vurdertIBehandling = behandling.id,
            opprettet = Instant.parse("2023-01-01T12:00:00Z")
        )

        dataSource.transaction {
            BarnepensjonRepositoryImpl(it).lagre(
                behandling.id,
                vurdering
            )
        }

        val hentet = dataSource.transaction {
            BarnepensjonRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(hentet).usingRecursiveComparison(sammenligner).isEqualTo(
            BarnepensjonGrunnlag(vurdering = vurdering)
        )

        dataSource.transaction {
            BarnepensjonRepositoryImpl(it).slett(behandling.id)
        }

        val hentetEtterSletting = dataSource.transaction {
            BarnepensjonRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(hentetEtterSletting).isNull()
    }

    @Test
    fun `hentHistoriskeVurderinger returnerer vurderinger fra tidligere behandlinger`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }

        // Opprett første behandling og lagre en barnepensjonvurdering
        val behandling1 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val vurdering1 = BarnepensjonVurdering(
            begrunnelse = "Første vurdering",
            perioder = setOf(
                BarnepensjonPeriode(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2022, 12),
                    månedsats = Beløp(10000)
                )
            ),
            vurdertAv = Bruker("Z123456"),
            vurdertIBehandling = behandling1.id,
            opprettet = Instant.parse("2023-01-01T12:00:00Z")
        )

        dataSource.transaction {
            BarnepensjonRepositoryImpl(it).lagre(behandling1.id, vurdering1)
        }

        // Avslutt første behandling og opprett en ny
        dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
        }

        val behandling2 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        // Hent historiske vurderinger for den nye behandlingen
        val historiskeVurderinger = dataSource.transaction {
            BarnepensjonRepositoryImpl(it).hentHistoriskeVurderinger(sak.id, behandling2.id)
        }

        assertThat(historiskeVurderinger).hasSize(1)
        assertThat(historiskeVurderinger.first())
            .usingRecursiveComparison(sammenligner)
            .isEqualTo(BarnepensjonGrunnlag(vurdering = vurdering1))
    }

    @Test
    fun `Kopier barnepensjon`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val vurdering = BarnepensjonVurdering(
            begrunnelse = "begrunnelse",
            perioder = setOf(
                BarnepensjonPeriode(
                    fom = YearMonth.of(2022, 1),
                    tom = YearMonth.of(2022, 12),
                    månedsats = Beløp(10000)
                ),
                BarnepensjonPeriode(
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 12),
                    månedsats = Beløp(25000)
                )
            ),
            vurdertAv = Bruker("Z123456"),
            vurdertIBehandling = behandling.id,
            opprettet = Instant.parse("2023-01-01T12:00:00Z")
        )

        dataSource.transaction {
            BarnepensjonRepositoryImpl(it).lagre(
                behandling.id,
                vurdering
            )
        }

        dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            val grunnlag2 = BarnepensjonRepositoryImpl(connection).hentHvisEksisterer(behandling2.id)
            assertThat(grunnlag2)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(BarnepensjonGrunnlag(vurdering = vurdering))
        }
    }
    
    
}
