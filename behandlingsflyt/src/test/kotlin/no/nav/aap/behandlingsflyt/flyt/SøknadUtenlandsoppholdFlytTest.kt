package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderSøknadUtenlandsoppholdLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadUtenlandsoppholdFlytTest : AbstraktFlytOrkestratorSnapshotTest(AlleAvskruddUnleash::class) {

    lateinit var sak: Sak

    @BeforeAll
    fun settOppFGB() = snapshotEtterSetup {
        sak = happyCaseFørstegangsbehandling(sendMeldekort = false)
    }

    @Test
    fun `søknad om opphold i utlandet gir egen behandling som avsluttes etter vurdering og vedtaksbrev`() {
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
            .løsAvklaringsBehov(VurderSøknadUtenlandsoppholdLøsning())
            .medKontekst {
                assertThat(behandling.aktivtSteg()).isEqualTo(StegType.BREV)
                assertThat(avklaringsbehovSomMåLøses.map { it.definisjon })
                    .containsOnly(Definisjon.SKRIV_VEDTAKSBREV)
            }
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)
            .medKontekst { assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET) }

        assertThat(hentBehandling(førstegangsbehandling.referanse).status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `søknad om opphold i utlandet kan opprettes manuelt`() {
        val behandling = sak.sendInnSøknadUtenlandsopphold(
            referanse = InnsendingReferanse(InnsendingReferanse.Type.MANUELL_OPPRETTELSE, UUID.randomUUID().toString())
        )

        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Utenlandsopphold)
        assertThat(behandling.aktivtSteg()).isEqualTo(StegType.VURDER_SØKNAD_UTENLANDSOPPHOLD)
    }

    @Test
    fun `hver søknad gir en ny behandling og samme journalpost behandles ikke to ganger`() {
        val referanse = InnsendingReferanse(journalpostId())

        val første = sak.sendInnSøknadUtenlandsopphold(referanse = referanse)
        sak.sendInnSøknadUtenlandsopphold(referanse = referanse)
        val andre = sak.sendInnSøknadUtenlandsopphold()

        assertThat(andre.id).isNotEqualTo(første.id)
        val utenlandsoppholdbehandlinger = dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection)
                .provide<BehandlingRepository>()
                .hentAlleFor(sak.id, listOf(TypeBehandling.Utenlandsopphold))
        }
        assertThat(utenlandsoppholdbehandlinger).hasSize(2)
    }
}
