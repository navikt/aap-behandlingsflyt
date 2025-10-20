package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.GosysService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Omgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryRefusjonKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class FatteVedtakStegTest {

    val klageresultatUtleder = mockk<KlageresultatUtleder>()
    val tidligereVurderinger = mockk<TidligereVurderinger>()
    val trekkKlageService = mockk<TrekkKlageService>()
    val gosysService = mockk<GosysService>()

    @BeforeEach
    fun setup() {
        every { trekkKlageService.klageErTrukket(any()) } returns false
    }

    @Test
    fun `Klagevurderinger fra Nay skal kvalitetssikres hvis delvis omgjøring `() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        every { klageresultatUtleder.utledKlagebehandlingResultat(BehandlingId(1L)) } returns DelvisOmgjøres(
            vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomSkalOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
        )
        every {
            tidligereVurderinger.girIngenBehandlingsgrunnlag(
                kontekst,
                StegType.FATTE_VEDTAK
            )
        } returns false

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_KLAGE_NAY,
            funnetISteg = StegType.KLAGEBEHANDLING_NAY,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val steg = FatteVedtakSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            tidligereVurderinger = tidligereVurderinger,
            klageresultatUtleder = klageresultatUtleder,
            trekkKlageService = trekkKlageService,
        )

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(FantAvklaringsbehov(Definisjon.FATTE_VEDTAK))
    }


    @Test
    fun `Klagevurderinger fra kontor skal ikke til beslutter om vedtaket opprettholdes`() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE),
            forrigeBehandlingId = null,
        )

        every { klageresultatUtleder.utledKlagebehandlingResultat(BehandlingId(1L)) } returns Opprettholdes(
            vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6)
        )
        every {
            tidligereVurderinger.girIngenBehandlingsgrunnlag(
                kontekst,
                StegType.FATTE_VEDTAK
            )
        } returns false

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_KLAGE_KONTOR,
            funnetISteg = StegType.KLAGEBEHANDLING_KONTOR,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val steg = FatteVedtakSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            tidligereVurderinger = tidligereVurderinger,
            klageresultatUtleder = klageresultatUtleder,
            trekkKlageService = trekkKlageService,
        )

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `Klagevurderinger skal kvalitetssikres hvis resultatet er Omgjør`() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE),
            forrigeBehandlingId = null,
        )

        every { klageresultatUtleder.utledKlagebehandlingResultat(BehandlingId(1L)) } returns Omgjøres(
            vilkårSomSkalOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6)
        )
        every {
            tidligereVurderinger.girIngenBehandlingsgrunnlag(
                kontekst,
                StegType.FATTE_VEDTAK
            )
        } returns false

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_KLAGE_NAY,
            funnetISteg = StegType.KLAGEBEHANDLING_NAY,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val steg = FatteVedtakSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            tidligereVurderinger = tidligereVurderinger,
            klageresultatUtleder = klageresultatUtleder,
            trekkKlageService = trekkKlageService,
        )

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(FantAvklaringsbehov(Definisjon.FATTE_VEDTAK))
    }
}