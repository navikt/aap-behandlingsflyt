package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.LocalDate
import java.util.*

class SendForvaltningsmeldingStegTest {

    private val sendForvaltningsmeldingSteg = SendForvaltningsmeldingSteg.konstruer(
        inMemoryRepositoryProvider,
        createGatewayProvider {
            register<FakeBrevbestillingGateway>()
            register<FakeUnleash>()
        }
    )

    @Test
    fun `steg før SendForvaltningsmeldingSteg skal ikke føre til avklaringsbehov som vil forsinke utsending`() {
        // Dersom det er definisjoner som ikke vil hindre i tilfeler det skal sendes forvaltningsmelding, så kan disse legges i unntak
        val unntak = listOf(Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT, Definisjon.VENTE_PÅ_KLAGE_IMPLEMENTASJON)

        assertThat(
            definisjonerSomLøsesFørSteg(
                Førstegangsbehandling.flyt(),
                StegType.SEND_FORVALTNINGSMELDING,
                unntak,
            )
        ).isEmpty()

        assertThat(
            definisjonerSomLøsesFørSteg(
                Revurdering.flyt(),
                StegType.SEND_FORVALTNINGSMELDING,
                unntak,
            ).isEmpty()
        )
    }

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = Mode.INCLUDE, names = ["Førstegangsbehandling", "Revurdering"])
    fun `sender en og kun en forvaltningsmelding for en behandling som har årsak MOTTATT_SØKNAD`(typeBehandling: TypeBehandling) {
        val behandling = opprettSakOgbehandlingForForvaltningsmelding(typeBehandling)
        val flytkontekst = flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)
        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).hasSize(1)
        assertThat(brevbestillinger.first().typeBrev).isEqualTo(TypeBrev.FORVALTNINGSMELDING)
    }

    @ParameterizedTest
    @EnumSource(Vurderingsbehov::class, mode = Mode.EXCLUDE, names = ["MOTTATT_SØKNAD"])
    fun `sender ikke forvaltningsmelding for en behandling som ikke har årsak MOTTATT_SØKNAD`(vurderingsbehov: Vurderingsbehov) {
        val behandling = opprettSakOgbehandlingForForvaltningsmelding(TypeBehandling.Førstegangsbehandling)
        val flytkontekst = flytkontekstForBehandlingForForvaltningsmelding(behandling, vurderingsbehov)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = Mode.EXCLUDE, names = ["Førstegangsbehandling", "Revurdering"])
    fun `sender ikke forvaltningsmelding for en behandling som ikke er førstegangsbehandling eller revurdering`(
        typeBehandling: TypeBehandling
    ) {
        val behandling = opprettSakOgbehandlingForForvaltningsmelding(typeBehandling)
        val flytkontekst = flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).isEmpty()
    }

    @Test
    fun `sender en og kun en klage mottatt for en behandling som har årsak MOTATT_KLAGE`() {
        val behandling = opprettSakOgbehandlingForKlageMottatt(TypeBehandling.Klage)
        val flytkontekst = flytkontekstForBehandlingForKlageMottatt(behandling, Vurderingsbehov.MOTATT_KLAGE)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)
        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).hasSize(1)
        assertThat(brevbestillinger.first().typeBrev).isEqualTo(TypeBrev.KLAGE_MOTTATT)
    }

    @ParameterizedTest
    @EnumSource(Vurderingsbehov::class, mode = Mode.EXCLUDE, names = ["MOTATT_KLAGE"])
    fun `sender ikke klage mottatt for en behandling som ikke har årsak MOTTATT_SØKNAD`(vurderingsbehov: Vurderingsbehov) {
        val behandling = opprettSakOgbehandlingForKlageMottatt(TypeBehandling.Klage)
        val flytkontekst = flytkontekstForBehandlingForKlageMottatt(behandling, vurderingsbehov)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = Mode.EXCLUDE, names = ["Klage"])
    fun `sender ikke klage mottatt for en behandling som ikke er klage`(typeBehandling: TypeBehandling) {
        val behandling = opprettSakOgbehandlingForKlageMottatt(typeBehandling)
        val flytkontekst = flytkontekstForBehandlingForKlageMottatt(behandling, Vurderingsbehov.MOTATT_KLAGE)

        sendForvaltningsmeldingSteg.utfør(flytkontekst)

        val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
        assertThat(brevbestillinger).isEmpty()
    }

    private fun definisjonerSomLøsesFørSteg(
        flyt: BehandlingFlyt,
        steg: StegType,
        unntak: List<Definisjon>,
    ): List<Definisjon> {
        return Definisjon.entries.filter {
            flyt.stegene().takeWhile { it != steg }
                .contains(it.løsesISteg) && !unntak.contains(it)
        }
    }

    private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))

    private fun opprettSakOgbehandlingForForvaltningsmelding(typeBehandling: TypeBehandling): Behandling {
        val person = Person(PersonId(1), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = InMemorySakRepository.finnEllerOpprett(person, periode)
        return InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            typeBehandling,
            null,
            VurderingsbehovOgÅrsak(
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                ÅrsakTilOpprettelse.SØKNAD
            )
        )
    }

    private fun flytkontekstForBehandlingForForvaltningsmelding(
        behandling: Behandling,
        vurderingsbehov: Vurderingsbehov
    ): FlytKontekstMedPerioder {
        return FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            vurderingsbehovRelevanteForSteg = setOf(vurderingsbehov),
            rettighetsperiode = periode,
        )
    }

    private fun opprettSakOgbehandlingForKlageMottatt(typeBehandling: TypeBehandling): Behandling {
        val person = Person(PersonId(1), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = InMemorySakRepository.finnEllerOpprett(person, periode)
        return InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            typeBehandling,
            null,
            VurderingsbehovOgÅrsak(
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTATT_KLAGE)),
                ÅrsakTilOpprettelse.KLAGE
            )
        )
    }

    private fun flytkontekstForBehandlingForKlageMottatt(
        behandling: Behandling,
        vurderingsbehov: Vurderingsbehov
    ): FlytKontekstMedPerioder {
        return FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            forrigeBehandlingId = behandling.forrigeBehandlingId,
            behandlingType = behandling.typeBehandling(),
            vurderingType = VurderingType.IKKE_RELEVANT,
            vurderingsbehovRelevanteForSteg = setOf(vurderingsbehov),
            rettighetsperiode = periode,
        )
    }
}
