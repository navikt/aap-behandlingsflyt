package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelse
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

        val hendelser = service.utledBehandlingVisningHendelser(behandling)

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
    fun `hendelsene har riktig sist endret i starttilstanden`() {
        val behandling = nyBehandling()

        assertThat(hendelse<BehandlingHendelse.SøknadMottatt>(behandling).sistEndret)
            .isEqualTo(behandling.opprettetTidspunkt)
        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).sistEndret).isNull()
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).sistEndret).isNull()
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).sistEndret).isNull()
        assertThat(hendelse<BehandlingHendelse.FerdigBehandlet>(behandling).sistEndret).isNull()
    }

    @Test
    fun `hendelsene bruker nyeste endring fra relevante avklaringsbehov`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        opprettAvklaringsbehov(behandling, Definisjon.MANUELL_OVERSTYRING_LOVVALG)
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .løsAvklaringsbehov(
                Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
                "Løst i test",
                Bruker("TEST"),
            )
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING)
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK)

        val avklaringsbehov = InMemoryAvklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id)
            .alle()
        val forventetLovvalgTidspunkt = avklaringsbehov
            .single { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP }
            .sistEndret()
        val forventetNavKontorTidspunkt = avklaringsbehov
            .single { it.definisjon == Definisjon.KVALITETSSIKRING }
            .sistEndret()
        val forventetNayTidspunkt = avklaringsbehov
            .single { it.definisjon == Definisjon.FATTE_VEDTAK }
            .sistEndret()

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).sistEndret)
            .isEqualTo(forventetLovvalgTidspunkt)
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).sistEndret)
            .isEqualTo(forventetNavKontorTidspunkt)
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).sistEndret)
            .isEqualTo(forventetNayTidspunkt)
        assertThat(hendelse<BehandlingHendelse.FerdigBehandlet>(behandling).sistEndret)
            .isEqualTo(forventetNayTidspunkt)
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
            StegType.AVKLAR_STUDENT_V2 to StegStatus.AVSLUTTER,
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
            StegType.AVKLAR_STUDENT_V2 to StegStatus.AVSLUTTER,
            StegType.VURDER_LOVVALG to StegStatus.AVSLUTTER,
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).sistEndret)
            .isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(3))
    }

    @Test
    fun `venting på utenlandske myndigheter gir status kun for alder lovvalg og medlemskap`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status)
            .isEqualTo(
                BehandlingHendelse.AlderLovvalgMedlemskap.Status
                    .VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
            )
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status).isNull()
    }

    @Test
    fun `venting på medisinske opplysninger gir status for NavKontor`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
        )

        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).status)
            .isEqualTo(BehandlingHendelse.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING)
    }

    @Test
    fun `avsluttet ventepunkt gir ikke status`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            avsluttet = true,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).status).isNull()
    }

    @Test
    fun `venteårsak uten mapping gir ikke status`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            venteårsak = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
        )

        assertThat(hendelse<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.NavKontor>(behandling).status).isNull()
        assertThat(hendelse<BehandlingHendelse.Nay>(behandling).status).isNull()
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
            StegType.AVKLAR_STUDENT_V2 to StegStatus.AVSLUTTER,
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
            StegType.AVKLAR_STUDENT_V2 to StegStatus.AVSLUTTER,
            StegType.AVKLAR_STUDENT_V2 to StegStatus.TILBAKEFØRT,
        )

        assertThat(progresjon<BehandlingHendelse.AlderLovvalgMedlemskap>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
    }

    @Test
    fun `åpent kvalitetssikringsbehov gir aktiv NavKontor-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING)

        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
    }

    @Test
    fun `avsluttet kvalitetssikringsbehov gir fullført NavKontor-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.KVALITETSSIKRING, avsluttet = true)

        assertThat(progresjon<BehandlingHendelse.NavKontor>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.FULLFØRT)
    }

    @Test
    fun `åpent vedtaksbehov gir aktiv Nay-progresjon`() {
        val behandling = nyBehandling()
        opprettAvklaringsbehov(behandling, Definisjon.FATTE_VEDTAK)

        assertThat(progresjon<BehandlingHendelse.Nay>(behandling))
            .isEqualTo(BehandlingHendelseProgresjon.AKTIV)
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
            perioderKanVurderes = null,
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

    private inline fun <reified T : BehandlingHendelse> progresjon(behandling: Behandling) =
        hendelse<T>(behandling).progresjon

    private inline fun <reified T : BehandlingHendelse> hendelse(behandling: Behandling) =
        service.utledBehandlingVisningHendelser(behandling)
            .filterIsInstance<T>()
            .single()
}
