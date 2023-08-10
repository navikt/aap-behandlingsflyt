package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.vedtak.FatteVedtakLøsning
import no.nav.aap.avklaringsbehov.vedtak.ForeslåVedtakLøsning
import no.nav.aap.avklaringsbehov.yrkesskade.AvklarYrkesskadeLøsning
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Førstegangsbehandling
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.behandling.Vilkårstype
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.grunnlag.person.Fødselsdato
import no.nav.aap.domene.behandling.grunnlag.person.PersonRegisterMock
import no.nav.aap.domene.behandling.grunnlag.person.Personinfo
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.domene.person.Personlager
import no.nav.aap.domene.sak.Sakslager
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType
import no.nav.aap.flyt.Tilstand
import no.nav.aap.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.hendelse.mottak.HendelsesMottak
import no.nav.aap.hendelse.mottak.LøsAvklaringsbehovBehandlingHendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FlytKontrollerTest {

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = Ident("123123123123")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        // Sender inn en søknad
        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))

        val sak = Sakslager.finnEllerOpprett(Personlager.finnEllerOpprett(ident), periode)
        val behandling = BehandlingTjeneste.finnSisteBehandlingFor(sak.id)
        assertThat(behandling?.type).isEqualTo(Førstegangsbehandling)

        assertThat(behandling?.avklaringsbehov()).isNotEmpty()
        assertThat(behandling?.status()).isEqualTo(Status.UTREDES)


        HendelsesMottak.håndtere(
            behandling?.id ?: 0L,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = AvklarYrkesskadeLøsning("Begrunnelse", "meg")
            )
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        assertThat(behandling?.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK }
        assertThat(behandling?.status()).isEqualTo(Status.UTREDES)

        HendelsesMottak.håndtere(
            behandling?.id ?: 0L,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = ForeslåVedtakLøsning("Begrunnelse", "meg")
            )
        )

        // Saken står til To-trinnskontroll hos beslutter
        assertThat(behandling?.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK }
        assertThat(behandling?.status()).isEqualTo(Status.UTREDES)

        HendelsesMottak.håndtere(
            behandling?.id ?: 0L,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = FatteVedtakLøsning("Begrunnelse", "meg")
            )
        )

        assertThat(behandling?.status()).isEqualTo(Status.AVSLUTTET)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = behandling?.vilkårsresultat()
        val aldersvilkår = vilkårsresultat?.finnVilkår(Vilkårstype.ALDERSVILKÅRET)

        assertThat(aldersvilkår?.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }
    }

    @Test
    fun `skal IKKE avklare yrkesskade hvis det finnes spor av yrkesskade`() {

        val ident = Ident("123123123124")
        val person = Personlager.finnEllerOpprett(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(18))))

        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))

        val sak = Sakslager.finnEllerOpprett(person, periode)
        val behandling = BehandlingTjeneste.finnSisteBehandlingFor(sak.id)
        assertThat(behandling?.type).isEqualTo(Førstegangsbehandling)

        assertThat(behandling?.avklaringsbehov()).isEmpty()
        assertThat(behandling?.status()).isEqualTo(Status.AVSLUTTET)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = behandling?.vilkårsresultat()
        val aldersvilkår = vilkårsresultat?.finnVilkår(Vilkårstype.ALDERSVILKÅRET)

        assertThat(aldersvilkår?.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }
    }

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`() {
        val ident = Ident("123123123125")
        val person = Personlager.finnEllerOpprett(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(17))))

        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))

        val sak = Sakslager.finnEllerOpprett(person, periode)
        val behandling = BehandlingTjeneste.finnSisteBehandlingFor(sak.id)
        assertThat(behandling?.type).isEqualTo(Førstegangsbehandling)

        val stegHistorikk = behandling?.stegHistorikk()
        assertThat(stegHistorikk?.map { it.tilstand }).contains(Tilstand(StegType.VURDER_ALDER, StegStatus.AVSLUTTER))

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = behandling?.vilkårsresultat()
        val aldersvilkår = vilkårsresultat?.finnVilkår(Vilkårstype.ALDERSVILKÅRET)

        assertThat(aldersvilkår?.vilkårsperioder())
            .hasSize(1)
            .noneMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }
    }
}
