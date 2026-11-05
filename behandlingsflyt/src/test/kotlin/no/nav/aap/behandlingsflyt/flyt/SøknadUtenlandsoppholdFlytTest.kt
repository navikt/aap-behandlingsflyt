package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftBrevSendtLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderSøknadUtenlandsoppholdLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utenlandsopphold.BekreftBrevSendtDto
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdResultat
import no.nav.aap.utenlandsopphold.SøknadUtenlandsoppholdVurderingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SøknadUtenlandsoppholdFlytTest : AbstraktFlytOrkestratorSnapshotTest(AlleAvskruddUnleash::class) {

    lateinit var sak: Sak

    @BeforeAll
    fun settOppFGB() = snapshotEtterSetup {
        sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
    }

    private val vurdering = SøknadUtenlandsoppholdVurderingDto(
        søknadsperiode = Periode(LocalDate.now(), LocalDate.now().plusDays(30)),
        land = "SE",
        resultat = SøknadUtenlandsoppholdResultat.INNVILGET,
        godkjentPeriode = Periode(LocalDate.now(), LocalDate.now().plusDays(14)),
        begrunnelse = "Begrunnelse",
    )

    @Test
    fun `søknad om opphold i utlandet gir egen behandling som avsluttes etter vurdering og bekreftet brev`() {
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        sak.sendInnSøknadUtenlandsopphold()
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Utenlandsopphold)
                assertThat(behandling.referanse).isNotEqualTo(førstegangsbehandling.referanse)
                assertThat(behandling.forrigeBehandlingId).isNull()
                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.VURDER_SØKNAD_UTENLANDSOPPHOLD)
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon })
                    .containsOnly(Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD)
            }
            .løsAvklaringsBehov(VurderSøknadUtenlandsoppholdLøsning(vurdering))
            .medKontekst {
                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.BEKREFT_BREV_SENDT)
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon })
                    .containsOnly(Definisjon.BEKREFT_BREV_SENDT_MANUELT)
            }
            .assertThrows(UgyldigForespørselException::class) {
                it.løsAvklaringsBehov(BekreftBrevSendtLøsning(BekreftBrevSendtDto(brevSendt = false)))
            }
            .medKontekst { assertThat(behandling.status()).isNotEqualTo(Status.AVSLUTTET) }
            .løsAvklaringsBehov(BekreftBrevSendtLøsning(BekreftBrevSendtDto(brevSendt = true)))
            .medKontekst { assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET) }

        assertThat(hentBehandling(førstegangsbehandling.referanse).status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `hver søknad gir en ny behandling og samme journalpost behandles ikke to ganger`() {
        val journalpostId = journalpostId()

        val første = sak.sendInnSøknadUtenlandsopphold(journalpostId = journalpostId)
        sak.sendInnSøknadUtenlandsopphold(journalpostId = journalpostId)
        val andre = sak.sendInnSøknadUtenlandsopphold()

        assertThat(andre.id).isNotEqualTo(første.id)
        val utenlandsoppholdbehandlinger = dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection)
                .provide<no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository>()
                .hentAlleFor(sak.id, listOf(TypeBehandling.Utenlandsopphold))
        }
        assertThat(utenlandsoppholdbehandlinger).hasSize(2)
    }

    @Test
    fun `dokumentet knyttes til behandlingen`() {
        val journalpostId = journalpostId()
        val behandling = sak.sendInnSøknadUtenlandsopphold(
            journalpostId = journalpostId,
            mottattTidspunkt = LocalDateTime.now().minusDays(2),
        )

        val dokument = dataSource.transaction { connection ->
            MottaDokumentService(postgresRepositoryRegistry.provider(connection).provide())
                .hentSøknadUtenlandsoppholdDokument(behandling.id)
        }

        assertThat(dokument!!.journalpostId).isEqualTo(journalpostId.identifikator)
        assertThat(dokument.mottattDato).isEqualTo(LocalDate.now().minusDays(2))
    }
}
