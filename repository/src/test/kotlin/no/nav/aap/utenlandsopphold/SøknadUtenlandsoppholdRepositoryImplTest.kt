package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@Tag("db")
internal class SøknadUtenlandsoppholdRepositoryImplTest {
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

    private val søknadsperiode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

    private fun vurdering(
        resultat: SøknadUtenlandsoppholdResultat = SøknadUtenlandsoppholdResultat.INNVILGET,
        godkjentPeriode: Periode? = søknadsperiode,
        antallGodkjenteDager: Int? = null,
    ) = SøknadUtenlandsoppholdVurdering(
        søknadsperiode = søknadsperiode,
        land = "SE",
        resultat = resultat,
        godkjentPeriode = godkjentPeriode,
        antallGodkjenteDager = antallGodkjenteDager,
        begrunnelse = "Begrunnelse",
        vurdertAv = Bruker("Z000000"),
        vurdertTidspunkt = LocalDateTime.of(2025, 2, 1, 12, 0),
    )

    @Test
    fun `lagrer og henter vurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = SøknadUtenlandsoppholdRepositoryImpl(connection)

            assertThat(repository.hentHvisEksisterer(behandling.id)).isNull()

            repository.lagre(behandling.id, vurdering())

            assertThat(repository.hentHvisEksisterer(behandling.id)!!.vurdering).isEqualTo(vurdering())
        }
    }

    @Test
    fun `ny vurdering deaktiverer den forrige`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = SøknadUtenlandsoppholdRepositoryImpl(connection)

            repository.lagre(behandling.id, vurdering())
            val avslag = vurdering(SøknadUtenlandsoppholdResultat.AVSLÅTT, godkjentPeriode = null)
            repository.lagre(behandling.id, avslag)

            assertThat(repository.hentHvisEksisterer(behandling.id)!!.vurdering).isEqualTo(avslag)
            val antallAktive = connection.queryFirst(
                "SELECT COUNT(*) AS antall FROM SOKNAD_UTENLANDSOPPHOLD_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV"
            ) {
                setParams { setLong(1, behandling.id.toLong()) }
                setRowMapper { it.getLong("antall") }
            }
            assertThat(antallAktive).isEqualTo(1)
        }
    }

    @Test
    fun `lagrer antall dager uten periode`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = SøknadUtenlandsoppholdRepositoryImpl(connection)

            val medDager = vurdering(godkjentPeriode = null, antallGodkjenteDager = 14)
            repository.lagre(behandling.id, medDager)

            assertThat(repository.hentHvisEksisterer(behandling.id)!!.vurdering).isEqualTo(medDager)
        }
    }

    @Test
    fun `kopier kopierer ikke grunnlaget`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val fra = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val til = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = SøknadUtenlandsoppholdRepositoryImpl(connection)
            repository.lagre(fra.id, vurdering())

            repository.kopier(fra.id, til.id)

            assertThat(repository.hentHvisEksisterer(til.id)).isNull()
        }
    }

    @Test
    fun `slett fjerner grunnlaget og er idempotent`() {
        dataSource.transaction { connection ->
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
            val repository = SøknadUtenlandsoppholdRepositoryImpl(connection)
            repository.lagre(behandling.id, vurdering())
            repository.lagre(behandling.id, vurdering(SøknadUtenlandsoppholdResultat.AVSLÅTT, godkjentPeriode = null))

            repository.slett(behandling.id)
            repository.slett(behandling.id)

            assertThat(repository.hentHvisEksisterer(behandling.id)).isNull()
        }
    }
}
