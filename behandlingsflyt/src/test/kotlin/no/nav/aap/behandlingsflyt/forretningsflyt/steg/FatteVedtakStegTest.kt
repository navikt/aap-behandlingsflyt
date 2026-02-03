package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.*
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraBeslutter
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
@MockKExtension.RequireParallelTesting
class FatteVedtakStegTest {

    val klageresultatUtleder = mockk<KlageresultatUtleder>(relaxed = true)
    val tidligereVurderinger = mockk<TidligereVurderinger>()
    val trekkKlageService = mockk<TrekkKlageService>()
    val avklaringsbehovService = mockk<AvklaringsbehovService>()
    val avbrytRevurderingService = mockk<AvbrytRevurderingService>()
    val trukketSøknadService = mockk<TrukketSøknadService>()
    val vedtakService = mockk<VedtakService>(relaxed = true)
    val virkningstidspunktUtleder = mockk<VirkningstidspunktUtleder>(relaxed = true)
    val gatewayProvider = createGatewayProvider {
        register<AlleAvskruddUnleash>()
    }

    @BeforeEach
    fun setup() {
        every { trekkKlageService.klageErTrukket(any()) } returns false
        every {
            avklaringsbehovService.oppdaterAvklaringsbehov(
                any(), any(), any(), any(), any()
            )
        } returns Unit
    }

    private fun kontekst(
        behandlingType: TypeBehandling,
        vurderingsbehov: Vurderingsbehov
    ) = flytKontekstMedPerioder {
        sakId = SakId(Random.nextLong())
        behandlingId = BehandlingId(Random.nextLong())
        this.behandlingType = behandlingType
        vurderingType = VurderingType.IKKE_RELEVANT
        rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))
        vurderingsbehovRelevanteForSteg = setOf(vurderingsbehov)
    }

    private fun steg(unleashGateway: UnleashGateway = AlleAvskruddUnleash) = FatteVedtakSteg(
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
        tidligereVurderinger = tidligereVurderinger,
        klageresultatUtleder = klageresultatUtleder,
        trekkKlageService = trekkKlageService,
        avklaringsbehovService = avklaringsbehovService,
        avbrytRevurderingService = avbrytRevurderingService,
        trukketSøknadService = trukketSøknadService,
        vedtakService = vedtakService,
        virkningstidspunktUtleder = virkningstidspunktUtleder,
        unleashGateway = unleashGateway,
    )

    @Test
    fun `Klagevurderinger fra Nay skal kvalitetssikres hvis delvis omgjøring`() {
        val kontekst = kontekst(behandlingType = TypeBehandling.Klage, vurderingsbehov = Vurderingsbehov.MOTATT_KLAGE)

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false

        val resultat = steg().utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `Klagevurderinger fra kontor skal ikke til beslutter om vedtaket opprettholdes`() {
        val kontekst = kontekst(behandlingType = TypeBehandling.Klage, vurderingsbehov = Vurderingsbehov.MOTATT_KLAGE)

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false

        val resultat = steg().utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `Klagevurderinger skal kvalitetssikres hvis resultatet er Omgjør`() {
        val kontekst = kontekst(behandlingType = TypeBehandling.Klage, vurderingsbehov = Vurderingsbehov.MOTATT_KLAGE)

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false

        val resultat = steg().utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = Mode.INCLUDE, names = ["Førstegangsbehandling", "Revurdering"])
    fun `lagrer vedtak`(typeBehandling: TypeBehandling) {
        val kontekst = kontekst(
            behandlingType = typeBehandling,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )

        every { trukketSøknadService.søknadErTrukket(kontekst.behandlingId) } returns false
        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        every { avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId) } returns false

        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )

        verify(exactly = 1) { vedtakService.lagreVedtak(kontekst.behandlingId, any(), any()) }
        assertThat(resultat).isEqualTo(Fullført)
    }

    @ParameterizedTest
    @EnumSource(TypeBehandling::class, mode = Mode.EXCLUDE, names = ["Førstegangsbehandling", "Revurdering"])
    fun `lagrer ikke vedtak`(typeBehandling: TypeBehandling) {
        val kontekst = kontekst(
            behandlingType = typeBehandling,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )

        verify(exactly = 0) { vedtakService.lagreVedtak(kontekst.behandlingId, any(), any()) }
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `lagrer vedtak for ytelsesbehandling med tidspunktet da fatte vedtak steget ble avsluttet etter godkjent totrinnsvurdering`() {
        val kontekst = kontekst(
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )
        val nå = LocalDateTime.now()
        val virkningstidspunkt = nå.toLocalDate().minusDays(1)

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        every { trukketSøknadService.søknadErTrukket(kontekst.behandlingId) } returns false
        every { virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId) } returns virkningstidspunkt

        opprettAvklaringsbehovMedEndringer(
            behandlingId = kontekst.behandlingId,
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            endringer = listOf(
                Endring(
                    status = Status.OPPRETTET,
                    tidsstempel = nå.plusMinutes(1),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.AVSLUTTET,
                    tidsstempel = nå.plusMinutes(2),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.SENDT_TILBAKE_FRA_BESLUTTER,
                    tidsstempel = nå.plusMinutes(5),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.TOTRINNS_VURDERT,
                    tidsstempel = nå.plusMinutes(7),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                )
            )
        )

        opprettAvklaringsbehovMedEndringer(
            behandlingId = kontekst.behandlingId,
            definisjon = Definisjon.FATTE_VEDTAK,
            endringer = listOf(
                Endring(
                    status = Status.OPPRETTET,
                    tidsstempel = nå.plusMinutes(3),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.AVSLUTTET,
                    tidsstempel = nå.plusMinutes(4),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.OPPRETTET,
                    tidsstempel = nå.plusMinutes(6),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.AVSLUTTET,
                    tidsstempel = nå.plusMinutes(8),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
            )
        )
        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )
        verify { vedtakService.lagreVedtak(kontekst.behandlingId, nå.plusMinutes(8), virkningstidspunkt) }
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `lagrer ikke vedtak dersom totrinnsvurdering ikke er godkjent`() {
        val kontekst = kontekst(
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        every { trukketSøknadService.søknadErTrukket(kontekst.behandlingId) } returns false

        opprettAvklaringsbehovMedEndringer(
            behandlingId = kontekst.behandlingId,
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            endringer = listOf(
                Endring(
                    status = Status.SENDT_TILBAKE_FRA_BESLUTTER,
                    tidsstempel = LocalDateTime.now().plusMinutes(1),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
            )
        )

        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )
        assertThat(resultat).isEqualTo(TilbakeføresFraBeslutter)
    }

    @Test
    fun `lagrer ikke vedtak dersom totrinnsvurdering ikke er godkjent enda`() {
        val kontekst = kontekst(
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )
        val nå = LocalDateTime.now()
        val virkningstidspunkt = nå.toLocalDate().minusDays(1)

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        every { trukketSøknadService.søknadErTrukket(kontekst.behandlingId) } returns false
        every { virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId) } returns virkningstidspunkt

        opprettAvklaringsbehovMedEndringer(
            behandlingId = kontekst.behandlingId,
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            endringer = listOf(
                Endring(
                    status = Status.SENDT_TILBAKE_FRA_BESLUTTER,
                    tidsstempel = nå.plusMinutes(3),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.AVSLUTTET,
                    tidsstempel = nå.plusMinutes(4),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
            )
        )

        opprettAvklaringsbehovMedEndringer(
            behandlingId = kontekst.behandlingId,
            definisjon = Definisjon.FATTE_VEDTAK,
            endringer = listOf(
                Endring(
                    status = Status.OPPRETTET,
                    tidsstempel = nå.plusMinutes(1),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.AVSLUTTET,
                    tidsstempel = nå.plusMinutes(2),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
            )
        )
        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )
        verify(exactly = 0) { vedtakService.lagreVedtak(kontekst.behandlingId, any(), any()) }
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `lagrer vedtak der det ikke er noen avklaringsbehov som krever totrinn`() {
        val kontekst = kontekst(
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )
        val nå = LocalDateTime.now()
        val virkningstidspunkt = nå.toLocalDate().minusDays(1)

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        every { trukketSøknadService.søknadErTrukket(kontekst.behandlingId) } returns false
        every { virkningstidspunktUtleder.utledVirkningsTidspunkt(kontekst.behandlingId) } returns virkningstidspunkt
        opprettAvklaringsbehovMedEndringer(
            behandlingId = kontekst.behandlingId,
            definisjon = Definisjon.SKRIV_SYKDOMSVURDERING_BREV,
            endringer = listOf(
                Endring(
                    status = Status.OPPRETTET,
                    tidsstempel = nå.plusMinutes(1),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
                Endring(
                    status = Status.AVSLUTTET,
                    tidsstempel = nå.plusMinutes(2),
                    begrunnelse = "Begrunnelse",
                    endretAv = "Ident",
                ),
            )
        )
        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )
        verify { vedtakService.lagreVedtak(kontekst.behandlingId, any(), any()) }
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `lagrer ikke vedtak for trukket førstegangsbehandling`() {
        val kontekst = kontekst(
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        every { trukketSøknadService.søknadErTrukket(kontekst.behandlingId) } returns true

        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )

        verify(exactly = 0) { vedtakService.lagreVedtak(kontekst.behandlingId, any(), any()) }
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `lagrer ikke vedtak for avbrutt revurdering`() {
        val kontekst = kontekst(
            behandlingType = TypeBehandling.Revurdering,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD
        )

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false
        every { avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId) } returns true
        val resultat =
            steg(FakeUnleashBaseWithDefaultDisabled(enabledFlags = listOf(BehandlingsflytFeature.LagreVedtakIFatteVedtak))).utfør(
                kontekst
            )

        verify(exactly = 0) { vedtakService.lagreVedtak(kontekst.behandlingId, any(), any()) }
        assertThat(resultat).isEqualTo(Fullført)
    }

    private fun opprettAvklaringsbehovMedEndringer(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        endringer: List<Endring>
    ) {
        InMemoryAvklaringsbehovRepository.opprett(
            behandlingId,
            definisjon = definisjon,
            funnetISteg = definisjon.løsesISteg,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehov =
            requireNotNull(InMemoryAvklaringsbehovRepository.hent(behandlingId).find { it.definisjon == definisjon })

        endringer.forEach { endring ->
            InMemoryAvklaringsbehovRepository.endre(
                avklaringsbehovId = avklaringsbehov.id,
                endring = endring
            )
        }
    }
}