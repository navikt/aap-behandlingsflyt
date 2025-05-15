package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Omgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Opprettholdes
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
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

    @BeforeEach
    fun setup() {
        every { trekkKlageService.klageErTrukket(any()) } returns false
    }

    @Test
    fun `Klagevurderinger fra Nay skal kvalitetssikres`() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            vurdering = mockk(relaxed = true),
            forrigeBehandlingId = null,
        )

        every { klageresultatUtleder.utledKlagebehandlingResultat(BehandlingId(1L)) } returns DelvisOmgjøres(
            vilkårSomSkalOpprettholdes = listOf(Hjemmel.FOLKETRYGDLOVEN_11_6),
            vilkårSomSkalOmgjøres= listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
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
            trekkKlageService = trekkKlageService
        )

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(FantAvklaringsbehov(Definisjon.FATTE_VEDTAK))
    }


    @Test
    fun `Klagevurderinger fra kontor skal kvalitetssikres`() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            vurdering = mockk(relaxed = true),
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
            trekkKlageService = trekkKlageService
        )

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(FantAvklaringsbehov(Definisjon.FATTE_VEDTAK))
    }
    
    @Test
    fun `Klagevurderinger skal ikke kvalitetssikres hvis resultatet er Omgjør`() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            vurdering = mockk(relaxed = true),
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
            trekkKlageService = trekkKlageService
        )

        val resultat = steg.utfør(kontekst)
        
        assertThat(resultat).isEqualTo(Fullført)
    }
}