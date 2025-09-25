package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.UnparsedStrukturertDokument
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDatabase
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbType
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime
import javax.sql.DataSource
import kotlin.test.Test

class HåndterUbehandledeDokumenterJobbUtførerTest {
    init {
        JobbType.leggTil(HåndterUbehandledeDokumenterJobbUtfører)
        JobbType.leggTil(HåndterUbehandletDokumentJobbUtfører)
    }

    @TestDatabase
    lateinit var dataSource: DataSource

    private val gatewayProvider = createGatewayProvider { FakeUnleash }

    @Test
    fun `Skal opprette håndteringsjobb for ubehandlede meldekort`() {
        val førstegangsbehandlingen = settOppFørstegangsbehandling()
        lagreMeldekortMottatt(
            referanse = InnsendingReferanse(JournalpostId("101")),
            mottattTidspunkt = (27 januar 2020).atStartOfDay(),
            meldekort = MeldekortV0(
                harDuArbeidet = true,
                timerArbeidPerPeriode = listOf(
                    ArbeidIPeriodeV0(
                        fraOgMedDato = 13 januar 2020,
                        tilOgMedDato = 24 januar 2020,
                        timerArbeid = 25.0,
                    )
                )
            ),
            førstegangsbehandlingen
        )

        dataSource.transaction { connection ->
            val repoprovider = postgresRepositoryRegistry.provider(connection)
            val mottattDokumentRepository = repoprovider
                .provide<MottattDokumentRepository>()
            val ubehandledeMeldekort = mottattDokumentRepository.hentAlleUbehandledeDokumenter()
            assertThat(ubehandledeMeldekort).hasSize(1)

            HåndterUbehandledeDokumenterJobbUtfører
                .konstruer(repoprovider, gatewayProvider)
                .utfør(JobbInput(HåndterUbehandledeDokumenterJobbUtfører))

            val jobber = hentJobber(connection)
            assertThat(jobber).hasSize(1)
            assertThat(jobber.first().sakId).isEqualTo(førstegangsbehandlingen.sakId)
            assertThat(jobber.first().type).isEqualTo(HåndterUbehandletDokumentJobbUtfører.type)
            assertThat(jobber.first().parameters.trimIndent()).isEqualTo(
                """
                innsendingsreferanse_verdi=101
                innsendingsreferanse_type=JOURNALPOST
                """.trimIndent()
            )
        }
    }

    private fun settOppFørstegangsbehandling() =
        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val sak = opprettSak(connection, Periode(1 januar 2020, 1 januar 2021))
            val førstegangsbehandlingen = finnEllerOpprettBehandling(connection, sak)
            repositoryProvider.provide<BehandlingRepository>()
                .oppdaterBehandlingStatus(førstegangsbehandlingen.id, Status.AVSLUTTET)
            førstegangsbehandlingen
        }

    private fun lagreMeldekortMottatt(
        referanse: InnsendingReferanse,
        mottattTidspunkt: LocalDateTime,
        meldekort: Meldekort,
        behandling: Behandling
    ) {

        val mottattMeldekort = MottattDokument(
            referanse = referanse,
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            mottattTidspunkt = mottattTidspunkt,
            type = InnsendingType.MELDEKORT,
            kanal = Kanal.DIGITAL,
            status = no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status.MOTTATT,
            strukturertDokument = UnparsedStrukturertDokument(
                DefaultJsonMapper.toJson(meldekort)
            )
        )
        dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection)
                .provide<MottattDokumentRepository>()
                .lagre(mottattMeldekort)
        }
    }

    private data class JobbInfo(
        val sakId: SakId,
        val type: String,
        val parameters: String

    )

    private fun hentJobber(connection: DBConnection): List<JobbInfo> {
        return connection.queryList(
            """
            SELECT * FROM JOBB
        """.trimIndent()
        ) {
            setRowMapper { row ->
                JobbInfo(
                    SakId(row.getLong("sak_id")),
                    row.getString("type"),
                    row.getString("parameters")
                )
            }
        }
    }
}