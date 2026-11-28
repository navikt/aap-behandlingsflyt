package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseNavn
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseProgresjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingVisningDatadelingServiceTest {
    private val service = BehandlingVisningDatadelingService(
        inMemoryRepositoryProvider,
        minimalGatewayProvider(),
    )

    @Test
    fun `hendelseslisten har riktig rekkefølge og progresjon i starttilstanden`() {
        val behandling = nyBehandling()

        val hendelser = service.utledBehandlingVisningHendelser(behandling, null)

        assertThat<Class<*>>(hendelser.map { it.javaClass }).containsExactly(
            BehandlingHendelse.SøknadMottatt::class.java,
            BehandlingHendelse.AlderLovvalgMedlemskap::class.java,
            BehandlingHendelse.NavKontor::class.java,
            BehandlingHendelse.Nay::class.java,
            BehandlingHendelse.FerdigBehandlet::class.java,
        )
        assertThat(hendelser.map { it.progresjon }).containsExactly(
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
        )
    }

    @Test
    fun `lovvalg er aktiv mens lovvalgsvurderingen pågår`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVKLARINGSPUNKT)

        assertThat(alleProgresjoner(behandling)).containsExactly(
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.AKTIV,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
        )
    }

    @Test
    fun `NavKontor er aktiv etter fullført lovvalgsvurdering`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
        )
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)

        assertThat(alleProgresjoner(behandling)).containsExactly(
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.AKTIV,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
        )
    }

    @Test
    fun `Nay er aktiv og NavKontor fullført når begge er påbegynt`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
        )
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)

        assertThat(alleProgresjoner(behandling)).containsExactly(
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.AKTIV,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
        )
    }

    @Test
    fun `bare Nay er aktiv når lovvalg, NavKontor og Nay er påbegynt`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVKLARINGSPUNKT)
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)

        assertThat(alleProgresjoner(behandling)).containsExactly(
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.AKTIV,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
        )
    }

    @Test
    fun `alle hendelser er fullført når behandlingen er avsluttet`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
        )
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM, avsluttet = true)
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING, avsluttet = true)
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT, avsluttet = true)
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK, avsluttet = true)
        oppdaterBehandlingStatus(behandling, BehandlingStatus.AVSLUTTET)

        assertThat(alleProgresjoner(behandling)).containsExactly(
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
        )
    }

    @Test
    fun `NavKontor vinner over aktiv lovvalgsvurdering når Nay ikke er påbegynt`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVKLARINGSPUNKT)
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)

        assertThat(alleProgresjoner(behandling)).containsExactly(
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.FULLFØRT,
            BehandlingHendelseProgresjon.AKTIV,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT,
        )
    }

    @Test
    fun `ikke påbegynt Nay returneres ikke når alt annet er fullført`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
        )
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM, avsluttet = true)
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING, avsluttet = true)
        oppdaterBehandlingStatus(behandling, BehandlingStatus.AVSLUTTET)

        val hendelser = service.utledBehandlingVisningHendelser(behandling, null)

        assertThat(hendelser.map { it.navn }).containsExactly(
            BehandlingHendelseNavn.SØKNAD_MOTTATT,
            BehandlingHendelseNavn.ALDER_LOVVALG_MEDLEMSKAP,
            BehandlingHendelseNavn.NAV_KONTOR,
            BehandlingHendelseNavn.FERDIG_BEHANDLET,
        )
        assertThat(hendelser.map { it.progresjon }).containsOnly(BehandlingHendelseProgresjon.FULLFØRT)
    }

    @Test
    fun `flere ikke påbegynte hendelser mellom fullførte hendelser returneres ikke`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK, avsluttet = true)

        val hendelser = service.utledBehandlingVisningHendelser(behandling, null)

        assertThat(hendelser.map { it.navn }).containsExactly(
            BehandlingHendelseNavn.SØKNAD_MOTTATT,
            BehandlingHendelseNavn.NAY,
            BehandlingHendelseNavn.FERDIG_BEHANDLET,
        )
    }

    @Test
    fun `ikke påbegynt hendelse mellom fullført og aktiv hendelse returneres ikke`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
        )
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)

        assertThat(service.utledBehandlingVisningHendelser(behandling, null).map { it.navn }).containsExactly(
            BehandlingHendelseNavn.SØKNAD_MOTTATT,
            BehandlingHendelseNavn.ALDER_LOVVALG_MEDLEMSKAP,
            BehandlingHendelseNavn.NAY,
            BehandlingHendelseNavn.FERDIG_BEHANDLET,
        )
    }

    @Test
    fun `relevant avklaringsbehov vinner over nyere steghistorikk`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        val forventetTidspunkt = InMemoryAvklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id)
            .alle()
            .single { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP }
            .sistEndret()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
            starttidspunkt = LocalDateTime.of(3000, 1, 1, 0, 0),
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).sistEndret)
            .isEqualTo(forventetTidspunkt)
    }

    @Test
    fun `ingen relevante behov og fullført progresjon bruker nyeste stegtidspunkt`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).sistEndret)
            .isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(2))
    }

    @Test
    fun `venting på utenlandske myndigheter gir status for aktiv lovvalgsvurdering`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVKLARINGSPUNKT)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status)
            .isEqualTo(
                BehandlingHendelse.AlderLovvalgMedlemskap.Status
                    .VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
            )
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status).isNull()
    }

    @Test
    fun `venting på utenlandske myndigheter gir ikke status før lovvalg og Nay er påbegynt`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.IKKE_PÅBEGYNT)
        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.IKKE_PÅBEGYNT)
        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status).isNull()
    }

    @Test
    fun `venting på utenlandske myndigheter gir ikke status for fullført lovvalgsvurdering`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER
        )
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status).isNull()
    }

    @Test
    fun `venting på utenlandske myndigheter gir ikke status for fullført Nay`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK, avsluttet = true)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
        )

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status).isNull()
    }

    @Test
    fun `venting på utenlandske myndigheter gir status for aktiv Nay`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
        )

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status)
            .isEqualTo(BehandlingHendelse.Nay.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER)
    }

    @Test
    fun `lovvalg mister utenlandsventestatus når NavKontor er aktiv`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVKLARINGSPUNKT)
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status).isNull()
        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
    }

    @Test
    fun `NavKontor mister legeerklæringsstatus når Nay er aktiv`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
        )

        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).status).isNull()
        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
    }

    @Test
    fun `venting på medisinske opplysninger gir status for aktiv NavKontor`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
        )

        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).status)
            .isEqualTo(BehandlingHendelse.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING)
    }

    @Test
    fun `avsluttet ventepunkt gir ikke ventestatus og Nay beholder ordinær status`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVKLARINGSPUNKT)
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            avsluttet = true,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status)
            .isEqualTo(BehandlingHendelse.Nay.Status.VENTER_PÅ_LEDIG_SAKSBEHANDLER)
    }

    @Test
    fun `venteårsak uten mapping gir ikke ventestatus og Nay beholder ordinær status`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVKLARINGSPUNKT)
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status)
            .isEqualTo(BehandlingHendelse.Nay.Status.VENTER_PÅ_LEDIG_SAKSBEHANDLER)
    }

    @Test
    fun `åpent beregningsbehov gir Nay-status venter på ledig saksbehandler uten ventepunkt`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status)
            .isEqualTo(BehandlingHendelse.Nay.Status.VENTER_PÅ_LEDIG_SAKSBEHANDLER)
    }

    @Test
    fun `avsluttet beregningsbehov gir Nay-status under behandling uten ventepunkt`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT, avsluttet = true)

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status)
            .isEqualTo(BehandlingHendelse.Nay.Status.UNDER_BEHANDLING)
    }

    @Test
    fun `ett avsluttet steg gir aktiv progresjon`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).sistEndret).isNull()
    }

    @Test
    fun `alle relevante steg avsluttet gir fullført progresjon`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
    }

    @Test
    fun `tilbakeført steg gjenåpner fullført progresjon`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(
            behandling,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.AVSLUTTER,
            StegType.VURDER_ALDER to StegStatus.TILBAKEFØRT,
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
    }

    @Test
    fun `sykdomsbehov med åpent kvalitetssikringsbehov gir aktiv NavKontor-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING)

        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
    }

    @Test
    fun `åpent kvalitetssikringsbehov uten sykdomsbehov gir ikke påbegynt NavKontor-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING)

        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.IKKE_PÅBEGYNT)
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).sistEndret).isNull()
    }

    @Test
    fun `avsluttet kvalitetssikringsbehov gir fullført NavKontor-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING, avsluttet = true)

        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
    }

    @Test
    fun `beregningsbehov med åpent vedtaksbehov gir aktiv Nay-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK)

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
    }

    @Test
    fun `åpent vedtaksbehov uten utløserbehov gir ikke påbegynt Nay-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK)

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.IKKE_PÅBEGYNT)
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).sistEndret).isNull()
    }

    @Test
    fun `avsluttet vedtaksbehov gir fullført Nay-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK, avsluttet = true)

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
    }

    @Test
    fun `åpen behandling med vedtaksbehov gir ikke påbegynt FerdigBehandlet-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK)

        assertThat(behandling.status()).isEqualTo(BehandlingStatus.OPPRETTET)
        assertThat(progresjon<BehandlingHendelse.FerdigBehandlet>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.IKKE_PÅBEGYNT)
        assertThat(hendelse<BehandlingHendelse.FerdigBehandlet>(behandling).sistEndret).isNull()
    }

    @Test
    fun `iverksatt behandling med vedtaksbehov gir fullført FerdigBehandlet-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK)
        oppdaterBehandlingStatus(behandling, BehandlingStatus.IVERKSETTES)

        assertThat(progresjon<BehandlingHendelse.FerdigBehandlet>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
    }

    @Test
    fun `avsluttet behandling med vedtaksbehov gir fullført FerdigBehandlet-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK)
        oppdaterBehandlingStatus(behandling, BehandlingStatus.AVSLUTTET)

        assertThat(progresjon<BehandlingHendelse.FerdigBehandlet>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
    }

    @Test
    fun `trukket søknad gir bare søknad mottatt og fullført FerdigBehandlet med trekkstatus`() {
        val behandling = nyBehandling()
        leggTilSteghistorikk(behandling, StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER)
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_SYKDOM)
        opprettAvklaringsbehov(behandling, Definisjon.VURDER_TREKK_AV_SØKNAD, avsluttet = true)
        val trekktidspunkt = InMemoryAvklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id)
            .alle()
            .single { it.definisjon == Definisjon.VURDER_TREKK_AV_SØKNAD }
            .sistEndret()

        val hendelser = service.utledBehandlingVisningHendelser(behandling, Resultat.TRUKKET)

        assertThat(hendelser.map { it.navn }).containsExactly(
            BehandlingHendelseNavn.SØKNAD_MOTTATT,
            BehandlingHendelseNavn.FERDIG_BEHANDLET,
        )
        val ferdigBehandlet = hendelser.filterIsInstance<BehandlingHendelse.FerdigBehandlet>().single()
        assertThat(behandling.status()).isEqualTo(BehandlingStatus.OPPRETTET)
        assertThat(ferdigBehandlet.progresjon).isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
        assertThat(ferdigBehandlet.status).isEqualTo(BehandlingHendelse.FerdigBehandlet.Status.SØKNAD_TRUKKET)
        assertThat(ferdigBehandlet.sistEndret).isEqualTo(trekktidspunkt)
    }

    private fun nyBehandling(): Behandling = opprettInMemorySakOgBehandling().second

    private fun leggTilSteghistorikk(
        behandling: Behandling,
        vararg tilstander: Pair<StegType, StegStatus>,
        starttidspunkt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0),
    ) {
        tilstander.forEachIndexed { index, (stegType, stegStatus) ->
            InMemoryBehandlingRepository.leggTilNyttAktivtSteg(
                behandlingId = behandling.id,
                tilstand = StegTilstand(
                    tidspunkt = starttidspunkt.plusSeconds(index.toLong()),
                    stegStatus = stegStatus,
                    stegType = stegType,
                ),
            )
        }
    }

    private fun opprettAvklaringsbehov(
        behandling: Behandling,
        definisjon: Definisjon,
        avsluttet: Boolean = false,
        venteårsak: ÅrsakTilSettPåVent? = null,
    ) {
        val avklaringsbehovene: Avklaringsbehovene =
            InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        avklaringsbehovene.opprett(
            definisjon = definisjon,
            funnetISteg = definisjon.løsesISteg,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            perioderVedtaketBehøverVurdering = null,
            perioderVilkåretErRelevant = null,
            begrunnelse = "Opprettet i test",
            bruker = Bruker("TEST"),
            grunn = venteårsak,
            gradBehov = null,
        )

        if (avsluttet) {
            avklaringsbehovene.løsAvklaringsbehov(definisjon, "Løst i test", Bruker("TEST"))
        }
    }

    private fun oppdaterBehandlingStatus(behandling: Behandling, status: BehandlingStatus) {
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(behandling.id, status)
        assertThat(behandling.status()).isEqualTo(status)
    }

    private fun alleProgresjoner(behandling: Behandling) =
        service.utledBehandlingVisningHendelser(behandling, null).map { it.progresjon }

    private inline fun <reified T : BehandlingHendelse> progresjon(behandling: Behandling) =
        hendelse<T>(behandling).progresjon

    private inline fun <reified T : BehandlingHendelse> hendelse(behandling: Behandling) =
        service.utledBehandlingVisningHendelser(behandling, null)
            .filterIsInstance<T>()
            .single()
}
