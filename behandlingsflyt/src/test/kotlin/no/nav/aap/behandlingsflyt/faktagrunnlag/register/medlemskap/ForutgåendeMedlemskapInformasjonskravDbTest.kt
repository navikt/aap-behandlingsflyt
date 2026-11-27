package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Tag("db")
@Fakes
class ForutgåendeMedlemskapInformasjonskravDbTest {
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
    fun `Identiske registerdata lagres ikke på nytt, endrede registerdata gir ny aktiv rad`() {
        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val sak = sak(connection, LocalDate.of(2025, 1, 1))
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val informasjonskrav = ForutgåendeMedlemskapInformasjonskrav.konstruer(
                repositoryProvider,
                testGatewayProvider(),
            )
            val kontekst = flytKontekstMedPerioder {
                this.behandling = behandling
                this.rettighetsperiode = sak.rettighetsperiode
            }
            val input = ForutgåendeMedlemskapInformasjonskrav.MedlemsskapInput(sak)

            val førsteGang = informasjonskrav.oppdater(input, forutgåendeRegisterdata(), kontekst)
            val antallEtterFørste = antallGrunnlagsrader(connection, behandling.id)

            val andreGang = informasjonskrav.oppdater(input, forutgåendeRegisterdata(), kontekst)
            val antallEtterAndre = antallGrunnlagsrader(connection, behandling.id)

            val tredjeGang = informasjonskrav.oppdater(input, forutgåendeRegisterdata(beløp = 20_000.0), kontekst)
            val antallEtterTredje = antallGrunnlagsrader(connection, behandling.id)

            assertThat(førsteGang).isEqualTo(Informasjonskrav.Endret.ENDRET)
            assertThat(andreGang).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
            assertThat(antallEtterAndre).describedAs("Ingen ny rad ved identiske data").isEqualTo(antallEtterFørste)
            assertThat(tredjeGang).isEqualTo(Informasjonskrav.Endret.ENDRET)
            assertThat(antallEtterTredje).isEqualTo(antallEtterFørste + 1)
        }
    }

    private fun antallGrunnlagsrader(connection: DBConnection, behandlingId: BehandlingId): Int =
        connection.queryFirst(
            "SELECT count(*) AS antall FROM FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG WHERE behandling_id = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getInt("antall") }
        }
}
