package no.nav.aap.behandlingsflyt.flyt.kontroll

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.domene.behandling.Status
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.grunnlag.person.Fødselsdato
import no.nav.aap.behandlingsflyt.grunnlag.person.PersonRegisterMock
import no.nav.aap.behandlingsflyt.grunnlag.person.Personinfo
import no.nav.aap.behandlingsflyt.grunnlag.yrkesskade.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.domene.person.Ident
import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.flyt.StegStatus
import no.nav.aap.behandlingsflyt.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.Tilstand
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.prosessering.Motor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FlytKontrollerTest {

    companion object {

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            Motor.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            Motor.stop()
        }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = Ident("123123123123")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        // Sender inn en søknad
        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        val sak = Sakslager.finnEllerOpprett(Personlager.finnEllerOpprett(ident), periode)
        val behandling = requireNotNull(BehandlingTjeneste.finnSisteBehandlingFor(sak.id))
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

        assertThat(behandling.avklaringsbehov()).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        HendelsesMottak.håndtere(
            behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = AvklarYrkesskadeLøsning(
                    yrkesskadevurdering = Yrkesskadevurdering(
                        begrunnelse = "Er syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                        erÅrsakssammenheng = false,
                        skadetidspunkt = null
                    )
                )
            )
        )
        ventPåSvar()

        HendelsesMottak.håndtere(
            behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = AvklarSykdomLøsning(
                    sykdomsvurdering = Sykdomsvurdering(
                        begrunnelse = "Er syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                        nedreGrense = NedreGrense.FEMTI,
                        nedsattArbeidsevneDato = LocalDate.now()
                    )
                )
            )
        )
        ventPåSvar()

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        assertThat(behandling.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        HendelsesMottak.håndtere(
            behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = ForeslåVedtakLøsning("Begrunnelse")
            )
        )
        ventPåSvar()

        // Saken står til To-trinnskontroll hos beslutter
        assertThat(behandling.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        HendelsesMottak.håndtere(
            behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = FatteVedtakLøsning("Begrunnelse")
            )
        )
        ventPåSvar()

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = behandling.vilkårsresultat()
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårstype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
    }

    private fun ventPåSvar() {
        while (Motor.harOppgaver()) {
            Thread.sleep(500L)
        }
    }

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`() {
        val ident = Ident("123123123125")
        val person = Personlager.finnEllerOpprett(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(17))))

        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        val sak = Sakslager.finnEllerOpprett(person, periode)
        val behandling = requireNotNull(BehandlingTjeneste.finnSisteBehandlingFor(sak.id))
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

        val stegHistorikk = behandling.stegHistorikk()
        assertThat(stegHistorikk.map { it.tilstand }).contains(Tilstand(StegType.VURDER_ALDER, StegStatus.AVSLUTTER))

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = behandling.vilkårsresultat()
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårstype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .noneMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val ident = Ident("123123123125")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        PersonRegisterMock.konstruer(ident, Personinfo(Fødselsdato(LocalDate.now().minusYears(20))))

        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        val sak = Sakslager.finnSakerFor(Personlager.finnEllerOpprett(ident)).single()
        val behandling = requireNotNull(BehandlingTjeneste.finnSisteBehandlingFor(sak.id))

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }

        HendelsesMottak.håndtere(
            behandling.id,
            BehandlingSattPåVent()
        )

        assertThat(behandling.status()).isEqualTo(Status.PÅ_VENT)
        assertThat(behandling.avklaringsbehov())
            .hasSize(2)
            .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
            .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }

        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.avklaringsbehov())
            .hasSize(2)
            .anySatisfy { !it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
            .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }

    }
}
