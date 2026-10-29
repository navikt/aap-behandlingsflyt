package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseProgresjon
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseType
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingVisningDto
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class BehandlingVisningDatadelingService(
    private val trukketSøknadService: TrukketSøknadService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    fun hendelseSøknadMottatt(behandling: Behandling): BehandlingHendelse.SøknadMottatt {
        return BehandlingHendelse.SøknadMottatt(
            sistEndret = behandling.opprettetTidspunkt,
            progresjon = BehandlingHendelseProgresjon.FULLFØRT
        )
    }

    fun hendelseAlderLovvalgMedlemskap(behandling: Behandling): BehandlingHendelse.AlderLovvalgMedlemskap {
        val stegTyper = listOf(StegType.VURDER_LOVVALG, StegType.AVKLAR_STUDENT_V2, StegType.VURDER_ALDER)
        val stegHistorikk = behandlingRepository.hentStegHistorikk(behandling.id)
            .filter { it.steg() == StegType.VURDER_LOVVALG || it.steg() == StegType.VURDER_ALDER || it.steg() == StegType.AVKLAR_STUDENT_V2 }
        val sisteTilstandPerSteg = stegTyper.associateWith { stegType ->
            stegHistorikk.lastOrNull { it.steg() == stegType }
        }

        val progresjon = when {
            sisteTilstandPerSteg.values.all { it?.status() == StegStatus.AVSLUTTER } ->
                BehandlingHendelseProgresjon.FULLFØRT

            sisteTilstandPerSteg.values.all { it == null } ->
                BehandlingHendelseProgresjon.IKKE_PÅBEGYNT

            else ->
                BehandlingHendelseProgresjon.AKTIV
        }
        val alleAvklaringsbehov = avklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id).alle()
        //TODO: utleder ikke fullført når lovvalg er vurdert automatisk
        val lovvalgMedlemskapAvklaringsbehov = alleAvklaringsbehov.filter {
            it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP || it.definisjon == Definisjon.MANUELL_OVERSTYRING_LOVVALG
        }
        val aktivtVentepunkt = alleAvklaringsbehov
            .find { it.erVentepunkt() && !it.erAvsluttet() }
            ?.let { behov ->
                when (behov.venteårsak()) {
                    ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelse.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
                    else -> null
                }
            }
        val sistEndret = when {
            lovvalgMedlemskapAvklaringsbehov.isNotEmpty() ->
                lovvalgMedlemskapAvklaringsbehov.maxOfOrNull { it.sistEndret() }

            progresjon == BehandlingHendelseProgresjon.FULLFØRT ->
                sisteTilstandPerSteg.values.mapNotNull { it?.tidspunkt() }.maxOrNull()

            else -> null
        }

        return BehandlingHendelse.AlderLovvalgMedlemskap(
            sistEndret = sistEndret,
            progresjon = progresjon,
            status = aktivtVentepunkt
        )
    }

    fun hendelseNavKontor(behandling: Behandling): BehandlingHendelse.NavKontor {
        val alleAvklaringsbehov = avklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id).alle()
        val navKontorAvklaringsbehov = alleAvklaringsbehov.filter {
            it.definisjon == Definisjon.KVALITETSSIKRING || it.definisjon == Definisjon.AVKLAR_SYKDOM
        }
        val aktivtVentepunkt = alleAvklaringsbehov
            .find { it.erVentepunkt() && !it.erAvsluttet() }
            ?.let { behov ->
                when (behov.venteårsak()) {
                    ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> BehandlingHendelse.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING
                    else -> null
                }
            }

        return BehandlingHendelse.NavKontor(
            sistEndret = navKontorAvklaringsbehov.maxOfOrNull { it.sistEndret() },
            progresjon = hendelseProgresjon(navKontorAvklaringsbehov),
            status = aktivtVentepunkt
        )
    }

    fun hendelseNay(behandling: Behandling): BehandlingHendelse.Nay {
        val alleAvklaringsbehov = avklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id).alle()
        val nayAvklaringsbehov = alleAvklaringsbehov.filter {
            it.definisjon == Definisjon.FATTE_VEDTAK || it.definisjon == Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT
        }

        return BehandlingHendelse.Nay(
            sistEndret = nayAvklaringsbehov.maxOfOrNull { it.sistEndret() },
            progresjon = hendelseProgresjon(nayAvklaringsbehov),
        )
    }

    fun hendelseFerdigBehandlet(behandling: Behandling): BehandlingHendelse.FerdigBehandlet {
        val alleAvklaringsbehov = avklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id).alle()
        val ferdigBehandletAvklaringsbehov = alleAvklaringsbehov.filter {
            it.definisjon == Definisjon.FATTE_VEDTAK
        }

        val progresjon = when {
            behandling.status().erAvsluttet() ->
                BehandlingHendelseProgresjon.FULLFØRT

            else ->
                BehandlingHendelseProgresjon.IKKE_PÅBEGYNT
        }
        return BehandlingHendelse.FerdigBehandlet(
            sistEndret = ferdigBehandletAvklaringsbehov.maxOfOrNull { it.sistEndret() },
            progresjon = progresjon,
        )
    }

    fun utledBehandlingVisningHendelser(behandling: Behandling): List<BehandlingHendelse> {
        return listOf(
            hendelseSøknadMottatt(behandling),
            hendelseAlderLovvalgMedlemskap(behandling),
            hendelseNavKontor(behandling),
            hendelseNay(behandling),
            hendelseFerdigBehandlet(behandling),
        )
    }

    fun utledBehandlingVisning(behandling: Behandling): BehandlingVisningDto? {
        val søknadErTrukket = trukketSøknadService.søknadErTrukket(behandling.id)
        if (behandling.erYtelsesbehandling() && behandling.årsakTilOpprettelse == ÅrsakTilOpprettelse.SØKNAD) {
            return BehandlingVisningDto(
                behandlingReferanse = behandling.referanse,
                avsluttet = søknadErTrukket || behandling.status().erAvsluttet(),
                behandlingType = BehandlingHendelseType.SØKNAD,
                hendelser = if (søknadErTrukket) {
                    emptyList()
                } else {
                    utledBehandlingVisningHendelser(behandling)
                }
            )
        }
        return null
    }

    private fun hendelseProgresjon(relevanteAvklaringsBehov: List<Avklaringsbehov>): BehandlingHendelseProgresjon {
        val avklaringsbehovOpprettetOgAvsluttet =
            relevanteAvklaringsBehov.size > 0 && relevanteAvklaringsBehov.all { it.status().erAvsluttet() }
        return if (avklaringsbehovOpprettetOgAvsluttet) {
            BehandlingHendelseProgresjon.FULLFØRT
        } else if (relevanteAvklaringsBehov.size > 0) {
            BehandlingHendelseProgresjon.AKTIV
        } else {
            BehandlingHendelseProgresjon.IKKE_PÅBEGYNT
        }

    }
}