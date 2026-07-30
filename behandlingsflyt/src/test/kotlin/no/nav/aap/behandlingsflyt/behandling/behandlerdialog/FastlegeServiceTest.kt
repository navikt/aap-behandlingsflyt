package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.flyt.TestSøknader
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FastlegeKontaktInformasjonDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.JaNei
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlerDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FastlegeDto as FastlegeFraSøknadDto

@Fakes
class FastlegeServiceTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            dataSource.close()
        }
    }

    @Test
    fun `fastlege er ikke endret siden søknad - returnerer erFastlegeEndretSidenSøknadstidspunkt false`() {
        val behandlerRef = UUID.randomUUID()
        val søknad = TestSøknader.STANDARD_SØKNAD.copy(
            fastlege = listOf(
                fastlegeFraSøknad(
                    erRegistrertFastlegeRiktig = JaNei.Ja,
                    behandlerRef = behandlerRef
                )
            )
        )
        val sak = opprettSakMedSøknad(søknad = søknad, registrertFastlege = behandler(behandlerRef))

        val resultat = utledFastlege(sak.saksnummer)

        assertThat(resultat.erFastlegeEndretSidenSøknadstidspunkt).isFalse()
        assertThat(resultat.varFastlegeRiktigPåSøknadstidspunkt).isTrue()
        assertThat(resultat.fastlege?.behandlerRef).isEqualTo(behandlerRef.toString())
    }

    @Test
    fun `fastlege er endret siden søknad - returnerer erFastlegeEndretSidenSøknadstidspunkt true`() {
        val fastlegeFraSøknad = fastlegeFraSøknad(
            erRegistrertFastlegeRiktig = JaNei.Ja,
            behandlerRef = UUID.randomUUID()
        )
        val søknad = TestSøknader.STANDARD_SØKNAD.copy(fastlege = listOf(fastlegeFraSøknad))
        val sak = opprettSakMedSøknad(søknad = søknad, registrertFastlege = behandler(behandlerRef = UUID.randomUUID()))

        val resultat = utledFastlege(sak.saksnummer)

        assertThat(resultat.erFastlegeEndretSidenSøknadstidspunkt).isTrue()
    }

    @Test
    fun `søker bekreftet at fastlege var feil på søknadstidspunktet - returnerer varFastlegeRiktigPåSøknadstidspunkt false`() {
        val fastlegeFraSøknad = fastlegeFraSøknad(erRegistrertFastlegeRiktig = JaNei.Nei)
        val søknad = TestSøknader.STANDARD_SØKNAD.copy(fastlege = listOf(fastlegeFraSøknad))
        val sak = opprettSakMedSøknad(søknad = søknad, registrertFastlege = behandler())

        val resultat = utledFastlege(sak.saksnummer)

        assertThat(resultat.varFastlegeRiktigPåSøknadstidspunkt).isFalse()
    }

    @Test
    fun `ingen registrert fastlege i register - returnerer erFastlegeEndretSidenSøknadstidspunkt true og fastlege null`() {
        val søknad = TestSøknader.STANDARD_SØKNAD.copy(fastlege = listOf(fastlegeFraSøknad(JaNei.Ja)))
        val sak = opprettSakMedSøknad(søknad = søknad, registrertFastlege = null)

        val resultat = utledFastlege(sak.saksnummer)

        assertThat(resultat.erFastlegeEndretSidenSøknadstidspunkt).isTrue()
        assertThat(resultat.fastlege).isNull()
    }

    private fun opprettSakMedSøknad(søknad: SøknadV0, registrertFastlege: BehandlerDto?): Sak {
        val person = FakePersoner.leggTil(TestPerson(fastlege = registrertFastlege))
        return dataSource.transaction { connection ->
            val dbPerson = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(person.aktivIdent()))
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(dbPerson, LocalDate.now())
            MottattDokumentRepositoryImpl(connection).lagre(
                MottattDokument(
                    referanse = InnsendingReferanse(JournalpostId(Random.nextInt(100000000, 999999999).toString())),
                    sakId = sak.id,
                    behandlingId = null,
                    mottattTidspunkt = LocalDateTime.now().minusMonths(1),
                    type = InnsendingType.SØKNAD,
                    kanal = Kanal.DIGITAL,
                    strukturertDokument = StrukturertDokument(søknad),
                )
            )
            sak
        }
    }

    private fun behandler(behandlerRef: UUID = UUID.randomUUID()): BehandlerDto {
        return BehandlerDto(
            behandlerRef = behandlerRef.toString(),
            hprId = behandlerRef.toString().take(10),
            fornavn = "fornavn",
            mellomnavn = "mellomnavn",
            etternavn = "etternavn",
            kontor = "kontor",
            adresse = "adresse",
            postnummer = "postnummer",
            poststed = "poststed",
            telefon = "telefon",
        )
    }

    private fun fastlegeFraSøknad(
        erRegistrertFastlegeRiktig: JaNei,
        behandlerRef: UUID = UUID.randomUUID(),
    ): FastlegeFraSøknadDto {
        return FastlegeFraSøknadDto(
            navn = "navn",
            behandlerRef = behandlerRef.toString(),
            kontaktinformasjon = FastlegeKontaktInformasjonDto(
                kontor = "kontor",
                adresse = "adresse",
                telefon = "telefon",
            ),
            erRegistrertFastlegeRiktig = erRegistrertFastlegeRiktig,
        )
    }

    private fun utledFastlege(saksnummer: Saksnummer): FastlegeResponse {
        return dataSource.transaction { connection ->
            FastlegeService(postgresRepositoryRegistry.provider(connection), defaultGatewayProvider())
                .utledFastlege(
                    saksnummer, OidcToken(
                        AzureTokenGen("behandlingsflyt")
                            .generate(isApp = false, azp = "behandlingsflyt")
                    )
                )
        }
    }
}