package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@MockKExtension.RequireParallelTesting
class FatteVedtakStegTest {

    val klageresultatUtleder = mockk<KlageresultatUtleder>(relaxed = true)
    val tidligereVurderinger = mockk<TidligereVurderinger>()
    val trekkKlageService = mockk<TrekkKlageService>()
    val avklaringsbehovService = mockk<AvklaringsbehovService>()
    val avbrytRevurderingService = mockk<AvbrytRevurderingService>()
    val trukketSøknadService = mockk<TrukketSøknadService>()
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

    private fun kontekst() = flytKontekstMedPerioder {
        this.behandlingType = TypeBehandling.Klage
        this.vurderingType = VurderingType.IKKE_RELEVANT
        this.rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))
        this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
    }

    private fun steg() = FatteVedtakSteg(
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
        tidligereVurderinger = tidligereVurderinger,
        klageresultatUtleder = klageresultatUtleder,
        trekkKlageService = trekkKlageService,
        avklaringsbehovService = avklaringsbehovService,
        avbrytRevurderingService = avbrytRevurderingService,
        trukketSøknadService = trukketSøknadService,
        vedtakService = mockk(relaxed = true),
        virkningstidspunktUtleder = mockk(relaxed = true),
        unleashGateway = AlleAvskruddUnleash,
    )

    @Test
    fun `Klagevurderinger fra Nay skal kvalitetssikres hvis delvis omgjøring`() {
        val kontekst = kontekst()

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false

        val resultat = steg().utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `Klagevurderinger fra kontor skal ikke til beslutter om vedtaket opprettholdes`() {
        val kontekst = kontekst()

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false

        val resultat = steg().utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `Klagevurderinger skal kvalitetssikres hvis resultatet er Omgjør`() {
        val kontekst = kontekst()

        every { tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, StegType.FATTE_VEDTAK) } returns false

        val resultat = steg().utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }
}