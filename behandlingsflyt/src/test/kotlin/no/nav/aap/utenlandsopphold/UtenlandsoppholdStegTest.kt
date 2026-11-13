package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BekreftBrevSendtLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.VurderSøknadUtenlandsoppholdLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftBrevSendtLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderSøknadUtenlandsoppholdLøsning
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySøknadUtenlandsoppholdRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UtenlandsoppholdStegTest {
    private val bruker = Bruker("LOKALKONTOR")
    private val vurderingDto = SøknadUtenlandsoppholdVurderingDto(
        søknadsperiode = Periode(1 januar 2025, 31 januar 2025),
        land = "SE",
        resultat = SøknadUtenlandsoppholdResultat.INNVILGET,
        godkjentPeriode = Periode(1 januar 2025, 14 januar 2025),
        begrunnelse = "Begrunnelse",
    )

    private fun vurderSteg() = VurderSøknadUtenlandsoppholdSteg.konstruer(
        inMemoryRepositoryProvider, minimalGatewayProvider()
    )

    private fun bekreftSteg() = BekreftBrevSendtSteg.konstruer(inMemoryRepositoryProvider, minimalGatewayProvider())

    private fun kontekst(behandling: Behandling) = flytKontekstMedPerioder {
        this.behandling = behandling
        this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.SØKNAD_UTENLANDSOPPHOLD)
    }

    private fun behov(behandling: Behandling, definisjon: Definisjon) =
        Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id).hentBehovForDefinisjon(definisjon)

    private fun løsVurdering(behandling: Behandling, dto: SøknadUtenlandsoppholdVurderingDto = vurderingDto) {
        VurderSøknadUtenlandsoppholdLøser(inMemoryRepositoryProvider).løs(
            AvklaringsbehovKontekst(bruker, behandling.flytKontekst()),
            VurderSøknadUtenlandsoppholdLøsning(dto)
        )
        Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id).løsAvklaringsbehov(
            Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD, "Begrunnelse", bruker, kreverToTrinn = false
        )
    }

    private fun løsBekreftelse(behandling: Behandling, brevSendt: Boolean = true) {
        BekreftBrevSendtLøser().løs(
            AvklaringsbehovKontekst(bruker, behandling.flytKontekst()),
            BekreftBrevSendtLøsning(BekreftBrevSendtDto(brevSendt))
        )
        Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id).løsAvklaringsbehov(
            Definisjon.BEKREFT_BREV_SENDT_MANUELT, "Brev sendt", bruker, kreverToTrinn = false
        )
    }

    @Test
    fun `vurderingssteget oppretter avklaringsbehov uten totrinn og lukkes når vurderingen er lagret`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = kontekst(behandling)

        assertThat(vurderSteg().utfør(kontekst)).isEqualTo(Fullført)
        val åpent = behov(behandling, Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD)
        assertThat(åpent).isNotNull
        assertThat(åpent!!.erÅpent()).isTrue
        assertThat(åpent.definisjon.kreverToTrinn).isFalse

        løsVurdering(behandling)
        vurderSteg().utfør(kontekst)

        assertThat(behov(behandling, Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD)!!.erÅpent()).isFalse
        assertThat(InMemorySøknadUtenlandsoppholdRepository.hentHvisEksisterer(behandling.id)!!.vurdering.vurdertAv)
            .isEqualTo(bruker)
    }

    @Test
    fun `bekreftelsessteget holder behandlingen åpen til brev er bekreftet sendt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val kontekst = kontekst(behandling)
        vurderSteg().utfør(kontekst)
        løsVurdering(behandling)

        bekreftSteg().utfør(kontekst)
        assertThat(behov(behandling, Definisjon.BEKREFT_BREV_SENDT_MANUELT)!!.erÅpent()).isTrue

        løsBekreftelse(behandling)
        bekreftSteg().utfør(kontekst)
        assertThat(behov(behandling, Definisjon.BEKREFT_BREV_SENDT_MANUELT)!!.erÅpent()).isFalse
        assertThat(behov(behandling, Definisjon.BEKREFT_BREV_SENDT_MANUELT)!!.løstAv()).isEqualTo(bruker)
    }

    @Test
    fun `løsningen avviser bekreftelse uten at brev er sendt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        assertThatThrownBy { løsBekreftelse(behandling, brevSendt = false) }
            .isInstanceOf(UgyldigForespørselException::class.java)
    }

    @Test
    fun `løsningen avviser ugyldig vurdering`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        assertThatThrownBy {
            løsVurdering(behandling, vurderingDto.copy(godkjentPeriode = null, antallGodkjenteDager = null))
        }.isInstanceOf(UgyldigForespørselException::class.java)
        assertThatThrownBy {
            løsVurdering(behandling, vurderingDto.copy(land = "XX"))
        }.isInstanceOf(UgyldigForespørselException::class.java)
        assertThat(InMemorySøknadUtenlandsoppholdRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `avklaringsbehovene har rett kode, steg og rolle`() {
        assertThat(Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD.kode.name).isEqualTo("4401")
        assertThat(Definisjon.BEKREFT_BREV_SENDT_MANUELT.kode.name).isEqualTo("4402")
        assertThat(Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD.løsesISteg)
            .isEqualTo(VurderSøknadUtenlandsoppholdSteg.type())
        assertThat(Definisjon.BEKREFT_BREV_SENDT_MANUELT.løsesISteg).isEqualTo(BekreftBrevSendtSteg.type())
    }
}
