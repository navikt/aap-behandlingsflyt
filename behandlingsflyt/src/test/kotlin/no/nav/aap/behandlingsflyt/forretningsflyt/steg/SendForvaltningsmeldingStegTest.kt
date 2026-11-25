package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.nesteHverdag
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaVedtakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeOppgavestyringGateway
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

object SendKlagebrevEnabledOgAltAnnetAvskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
    emptyList()
)

object ForvaltningsmeldingKantIKantPåskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
    listOf(BehandlingsflytFeature.NykantIkantForvaltningsmelding)
)

object KunMaksdatoForKantTilKantPåskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
    listOf(BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)
)

class SendForvaltningsmeldingStegTest {
    private val sendForvaltningsmeldingSteg = SendForvaltningsmeldingSteg.konstruer(
        inMemoryRepositoryProvider,
        createGatewayProvider {
            register<FakeBrevbestillingGateway>()
            register<FakeOppgavestyringGateway>()
            register<SendKlagebrevEnabledOgAltAnnetAvskruddUnleash>()
        }
    )

    @Test
    fun `steg før SendForvaltningsmeldingSteg skal ikke føre til avklaringsbehov som vil forsinke utsending`() {
        // Dersom det er definisjoner som ikke vil hindre i tilfeler det skal sendes forvaltningsmelding, så kan disse legges i unntak
        val unntak = listOf(
            Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT,
            Definisjon.VENTE_PÅ_KLAGE_IMPLEMENTASJON,
            Definisjon.VURDER_KRAV,
        )

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

    @Nested
    inner class Forvaltningsmelding {

        @ParameterizedTest
        @EnumSource(TypeBehandling::class, mode = Mode.INCLUDE, names = ["Førstegangsbehandling", "Revurdering"])
        fun `sender en og kun en forvaltningsmelding for en behandling som har årsak MOTTATT_SØKNAD`(typeBehandling: TypeBehandling) {
            val behandling = opprettSakOgbehandlingForForvaltningsmelding(typeBehandling)
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

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
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

            sendForvaltningsmeldingSteg.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger).isEmpty()
        }
    }

    @Nested
    inner class ForvaltningsmeldingKantIKant {
        private val stegMedKantIKant = SendForvaltningsmeldingSteg.konstruer(
            inMemoryRepositoryProvider,
            createGatewayProvider {
                register<FakeBrevbestillingGateway>()
                register<FakeOppgavestyringGateway>()
                register<ForvaltningsmeldingKantIKantPåskruddUnleash>()
            }
        )

        @Test
        fun `sender en og kun en kant i kant-forvaltningsmelding når søknaden er kant i kant med Arena`() {
            val behandling = opprettSakOgbehandlingForForvaltningsmelding(TypeBehandling.Førstegangsbehandling)
            lagreKravOgArenaMaksdato(behandling.id, muligRettFra = DAGEN_ETTER_MAKSDATO)
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

            stegMedKantIKant.utfør(flytkontekst)
            stegMedKantIKant.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger.map { it.typeBrev })
                .containsExactly(TypeBrev.FORVALTNINGSMELDING_KANT_I_KANT)
        }

        @Test
        fun `sender vanlig forvaltningsmelding når det er opphold mellom Arena-saken og kravet`() {
            val behandling = opprettSakOgbehandlingForForvaltningsmelding(TypeBehandling.Førstegangsbehandling)
            lagreKravOgArenaMaksdato(behandling.id, muligRettFra = MAKSDATO.nesteHverdag().plusDays(1))
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

            stegMedKantIKant.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger.map { it.typeBrev }).containsExactly(TypeBrev.FORVALTNINGSMELDING)
        }

        @Test
        fun `sender vanlig forvaltningsmelding når toggle for kant i kant er av`() {
            val behandling = opprettSakOgbehandlingForForvaltningsmelding(TypeBehandling.Førstegangsbehandling)
            lagreKravOgArenaMaksdato(behandling.id, muligRettFra = DAGEN_ETTER_MAKSDATO)
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

            sendForvaltningsmeldingSteg.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger.map { it.typeBrev }).containsExactly(TypeBrev.FORVALTNINGSMELDING)
        }

        @Test
        fun `sender vanlig forvaltningsmelding når toggle for ny kant i kant-forvaltningsmelding er av`() {
            val steg = SendForvaltningsmeldingSteg.konstruer(
                inMemoryRepositoryProvider,
                createGatewayProvider {
                    register<FakeBrevbestillingGateway>()
                    register<FakeOppgavestyringGateway>()
                    register<KunMaksdatoForKantTilKantPåskruddUnleash>()
                }
            )
            val behandling = opprettSakOgbehandlingForForvaltningsmelding(TypeBehandling.Førstegangsbehandling)
            lagreKravOgArenaMaksdato(behandling.id, muligRettFra = DAGEN_ETTER_MAKSDATO)
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)

            steg.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger.map { it.typeBrev }).containsExactly(TypeBrev.FORVALTNINGSMELDING)
        }

        @Test
        fun `sender vanlig forvaltningsmelding i revurdering selv om Arena-maksdatoen gir kant i kant`() {
            val førstegangsbehandling =
                opprettSakOgbehandlingForForvaltningsmelding(TypeBehandling.Førstegangsbehandling)
            val revurdering = InMemoryBehandlingRepository.opprettBehandling(
                førstegangsbehandling.sakId,
                TypeBehandling.Revurdering,
                førstegangsbehandling.id,
                VurderingsbehovOgÅrsak(
                    listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                    ÅrsakTilOpprettelse.SØKNAD
                )
            )
            lagreKravOgArenaMaksdato(revurdering.id, muligRettFra = DAGEN_ETTER_MAKSDATO)
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(revurdering, Vurderingsbehov.MOTTATT_SØKNAD)

            stegMedKantIKant.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(revurdering.id)
            assertThat(brevbestillinger.map { it.typeBrev }).containsExactly(TypeBrev.FORVALTNINGSMELDING)
        }

        @Test
        fun `sender ikke kant i kant-forvaltningsmelding når vanlig forvaltningsmelding allerede er bestilt`() {
            val behandling = opprettSakOgbehandlingForForvaltningsmelding(TypeBehandling.Førstegangsbehandling)
            val flytkontekst =
                flytkontekstForBehandlingForForvaltningsmelding(behandling, Vurderingsbehov.MOTTATT_SØKNAD)
            stegMedKantIKant.utfør(flytkontekst)

            lagreKravOgArenaMaksdato(behandling.id, muligRettFra = DAGEN_ETTER_MAKSDATO)
            stegMedKantIKant.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger.map { it.typeBrev }).containsExactly(TypeBrev.FORVALTNINGSMELDING)
        }

        private fun lagreKravOgArenaMaksdato(behandlingId: BehandlingId, muligRettFra: LocalDate) {
            InMemoryKravRepository.lagre(
                behandlingId,
                setOf(
                    RelevantKrav(
                        referanse = Kravreferanse.ny(),
                        journalpostId = JournalpostId("123"),
                        vurdertAv = SYSTEMBRUKER,
                        begrunnelse = "Test",
                        vurdertIBehandling = behandlingId,
                        opprettet = Instant.now(),
                        søknadsdato = Søknadsdato(muligRettFra, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
                        overstyrMuligRettFra = null,
                        muligRettFra = muligRettFra,
                    )
                )
            )
            InMemoryArenaMaksdatoRepository.lagre(
                behandlingId,
                ArenaMaksdato(
                    arenaSaksnummer = "2023-5",
                    vedtakStatus = ArenaVedtakStatus.AVSLUTTET,
                    maksdato = MAKSDATO,
                    vedtakTil = MAKSDATO,
                    vedtakId = 1,
                    harForlengelseEtter11_12 = false,
                )
            )
        }
    }

    @Nested
    inner class KlageMottattmelding {
        @Test
        fun `sender en og kun en klage mottatt melding for en journalpost innsendt klage som har årsak MOTATT_KLAGE`() {
            val behandling = opprettSakOgbehandlingForKlageMottatt(TypeBehandling.Klage)
            val flytkontekst = flytkontekstForBehandlingForKlageMottatt(behandling, Vurderingsbehov.MOTATT_KLAGE)
            opprettKlageDokument(behandling.sakId, behandling.id, InnsendingReferanse.Type.JOURNALPOST)

            sendForvaltningsmeldingSteg.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger).hasSize(1)
            assertThat(brevbestillinger.first().typeBrev).isEqualTo(TypeBrev.KLAGE_MOTTATT)
        }

        @ParameterizedTest
        @EnumSource(InnsendingReferanse.Type::class, mode = Mode.EXCLUDE, names = ["JOURNALPOST"])
        fun `sender kun klage mottatt melding for en klage som har innsendt referanse av type JOURNALPOST`(
            typeInnsendingReferanse: InnsendingReferanse.Type
        ) {
            val behandling = opprettSakOgbehandlingForKlageMottatt(TypeBehandling.Klage)
            val flytkontekst = flytkontekstForBehandlingForKlageMottatt(behandling, Vurderingsbehov.MOTATT_KLAGE)
            opprettKlageDokument(behandling.sakId, behandling.id, typeInnsendingReferanse)

            sendForvaltningsmeldingSteg.utfør(flytkontekst)
            sendForvaltningsmeldingSteg.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger).isEmpty()
        }

        @ParameterizedTest
        @EnumSource(Vurderingsbehov::class, mode = Mode.EXCLUDE, names = ["MOTATT_KLAGE"])
        fun `sender ikke klage mottatt for en behandling som ikke har årsak MOTTATT_KLAGE`(vurderingsbehov: Vurderingsbehov) {
            val behandling = opprettSakOgbehandlingForKlageMottatt(TypeBehandling.Klage)
            val flytkontekst = flytkontekstForBehandlingForKlageMottatt(behandling, vurderingsbehov)
            opprettKlageDokument(behandling.sakId, behandling.id, InnsendingReferanse.Type.JOURNALPOST)

            sendForvaltningsmeldingSteg.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger).isEmpty()
        }

        @ParameterizedTest
        @EnumSource(TypeBehandling::class, mode = Mode.EXCLUDE, names = ["Klage"])
        fun `sender ikke klage mottatt for en behandling som ikke er klage`(typeBehandling: TypeBehandling) {
            val behandling = opprettSakOgbehandlingForKlageMottatt(typeBehandling)
            val flytkontekst = flytkontekstForBehandlingForKlageMottatt(behandling, Vurderingsbehov.MOTATT_KLAGE)
            opprettKlageDokument(behandling.sakId, behandling.id, InnsendingReferanse.Type.JOURNALPOST)

            sendForvaltningsmeldingSteg.utfør(flytkontekst)

            val brevbestillinger = InMemoryBrevbestillingRepository.hent(behandling.id)
            assertThat(brevbestillinger).isEmpty()
        }
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
        val sak = opprettInMemorySak(søknadsdato = periode.fom)
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
        return flytKontekstMedPerioder {
            this.behandling = behandling
            vurderingsbehovRelevanteForSteg = setOf(vurderingsbehov)
            this.rettighetsperiode = periode
        }
    }

    private fun opprettSakOgbehandlingForKlageMottatt(typeBehandling: TypeBehandling): Behandling {
        val sak = opprettInMemorySak(periode.fom)
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

    private fun opprettKlageDokument(
        sakId: SakId,
        behandlingId: BehandlingId,
        typeInnsendingReferanse: InnsendingReferanse.Type
    ) {
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(typeInnsendingReferanse, "uuid-referanse"),
                sakId = sakId,
                behandlingId = behandlingId,
                mottattTidspunkt = LocalDateTime.now(),
                type = InnsendingType.KLAGE,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null
            )
        )
    }

    private fun flytkontekstForBehandlingForKlageMottatt(
        behandling: Behandling,
        vurderingsbehov: Vurderingsbehov
    ): FlytKontekstMedPerioder {
        return flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingsbehovRelevanteForSteg = setOf(vurderingsbehov)
            this.rettighetsperiode = periode
        }
    }

    private companion object {
        val MAKSDATO: LocalDate = LocalDate.of(2026, 1, 31)
        val DAGEN_ETTER_MAKSDATO: LocalDate = MAKSDATO.plusDays(1)
    }
}
