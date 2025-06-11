package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.LocalDate
import java.util.*

class SendForvaltningsmeldingStegTest {

    private val sendForvaltningsmeldingSteg = SendForvaltningsmeldingSteg(
        brevbestillingService = BrevbestillingService(
            signaturService = SignaturService(avklaringsbehovRepository = InMemoryAvklaringsbehovRepository),
            brevbestillingGateway = FakeBrevbestillingGateway(),
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository = InMemoryBehandlingRepository,
            sakRepository = InMemorySakRepository,
        ),
        InMemoryBehandlingRepository,
        FakeUnleash
    )

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = Mode.INCLUDE, names = ["Førstegangsbehandling", "Revurdering"])
    fun `sender en og kun en forvaltningsmelding for en behandling som har årsak MOTTATT_SØKNAD`(typeBehandling: TypeBehandling) {
        val behandling = opprettSakOgbehandling(typeBehandling)
        val flytkontekst = flytkontekstForBehandling(behandling, ÅrsakTilBehandling.MOTTATT_SØKNAD)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)
        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).hasSize(1)
        assertThat(brevbestillinger.first().typeBrev).isEqualTo(TypeBrev.FORVALTNINGSMELDING)
    }

    @ParameterizedTest
    @EnumSource(ÅrsakTilBehandling::class, mode = Mode.EXCLUDE, names = ["MOTTATT_SØKNAD"])
    fun `sender ikke forvaltningsmelding for en behandling som har årsak MOTTATT_SØKNAD`(årsakTilBehandling: ÅrsakTilBehandling) {
        val behandling = opprettSakOgbehandling(TypeBehandling.Førstegangsbehandling)
        val flytkontekst = flytkontekstForBehandling(behandling, årsakTilBehandling)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = Mode.EXCLUDE, names = ["Førstegangsbehandling", "Revurdering"])
    fun `sender ikke forvaltningsmelding for en behandling som ikke er førstegangsbehandling eller revurdering`(typeBehandling: TypeBehandling) {
        val behandling = opprettSakOgbehandling(typeBehandling)
        val flytkontekst = flytkontekstForBehandling(behandling, ÅrsakTilBehandling.MOTTATT_SØKNAD)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).isEmpty()
    }

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

    private fun opprettSakOgbehandling(typeBehandling: TypeBehandling): Behandling {
        val person = Person(1, UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = InMemorySakRepository.finnEllerOpprett(person, periode)
        return InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            listOf(),
            typeBehandling,
            null
        )
    }

    private fun flytkontekstForBehandling(behandling: Behandling, årsakTilBehandling: ÅrsakTilBehandling): FlytKontekstMedPerioder {
        return FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            årsakerTilBehandling = setOf(årsakTilBehandling),
            rettighetsperiode = periode,
        )
    }
}
