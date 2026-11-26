package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
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
        val stegTyper = listOf(StegType.VURDER_LOVVALG, StegType.VURDER_ALDER)
        val stegHistorikk = behandlingRepository.hentStegHistorikk(behandling.id)
            .filter { it.steg() == StegType.VURDER_LOVVALG || it.steg() == StegType.VURDER_ALDER }
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
        val lovvalgMedlemskapAvklaringsbehov = alleAvklaringsbehov.filter {
            it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP || it.definisjon == Definisjon.MANUELL_OVERSTYRING_LOVVALG
        }
        val sistEndret = when {
            lovvalgMedlemskapAvklaringsbehov.isNotEmpty() ->
                lovvalgMedlemskapAvklaringsbehov.maxOfOrNull { it.sistEndret() }

            progresjon == BehandlingHendelseProgresjon.FULLFØRT ->
                sisteTilstandPerSteg.values.mapNotNull { it?.tidspunkt() }.maxOrNull()

            else -> null
        }
        val aktivtVentepunkt = alleAvklaringsbehov
            .find { it.erVentepunkt() && !it.erAvsluttet() }
            ?.let { behov ->
                when (behov.venteårsak()) {
                    ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelse.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
                    else -> null
                }
            }

        return BehandlingHendelse.AlderLovvalgMedlemskap(
            sistEndret = sistEndret,
            progresjon = progresjon,
            status = when (progresjon) {
                BehandlingHendelseProgresjon.AKTIV -> aktivtVentepunkt
                else -> null
            }
        )
    }

    fun hendelseNavKontor(behandling: Behandling): BehandlingHendelse.NavKontor {
        val alleAvklaringsbehov = avklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id).alle()
        val progresjon = when {
            alleAvklaringsbehov.find { it.definisjon == Definisjon.KVALITETSSIKRING }?.status() == Status.AVSLUTTET &&
                    alleAvklaringsbehov.filter { it.status() === Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER }.isEmpty() ->
                BehandlingHendelseProgresjon.FULLFØRT

            alleAvklaringsbehov.find { it.definisjon == Definisjon.AVKLAR_SYKDOM } != null ->
                BehandlingHendelseProgresjon.AKTIV

            else ->
                BehandlingHendelseProgresjon.IKKE_PÅBEGYNT
        }
        val aktivtVentepunkt = alleAvklaringsbehov
            .find { it.erVentepunkt() && !it.erAvsluttet() }
            ?.let { behov ->
                when (behov.venteårsak()) {
                    ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> BehandlingHendelse.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING
                    else -> null
                }
            }
        val sistEndret = if (progresjon == BehandlingHendelseProgresjon.IKKE_PÅBEGYNT) {
            null
        } else {
            alleAvklaringsbehov.filter { it.erAvsluttet() }.maxOfOrNull { it.sistEndret() }
        }

        return BehandlingHendelse.NavKontor(
            sistEndret = sistEndret,
            progresjon = progresjon,
            status = aktivtVentepunkt
        )
    }

    fun hendelseNay(behandling: Behandling): BehandlingHendelse.Nay {
        val definisjonsListe = listOf(
            Definisjon.AVKLAR_STUDENT_V2,
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            Definisjon.AVKLAR_YRKESSKADE,
            Definisjon.AVKLAR_SYKEPENGEERSTATNING
        )
        val alleAvklaringsbehov = avklaringsbehovRepository
            .hentAvklaringsbehovene(behandling.id).alle()
        val avklaringsbehovSomTriggerUnderBehandling = alleAvklaringsbehov.filter {
            definisjonsListe.contains(it.definisjon)
        }
        val progresjon = when {
            alleAvklaringsbehov.find { it.definisjon == Definisjon.FATTE_VEDTAK }?.status() == Status.AVSLUTTET &&
                    alleAvklaringsbehov.filter { it.status() === Status.SENDT_TILBAKE_FRA_BESLUTTER }.isEmpty() ->
                BehandlingHendelseProgresjon.FULLFØRT

            alleAvklaringsbehov.find { definisjonsListe.contains(it.definisjon) } != null ->
                BehandlingHendelseProgresjon.AKTIV

            else ->
                BehandlingHendelseProgresjon.IKKE_PÅBEGYNT
        }
        val status = when {
            avklaringsbehovSomTriggerUnderBehandling.isNotEmpty() && avklaringsbehovSomTriggerUnderBehandling.all { it.status() == Status.OPPRETTET } ->
                BehandlingHendelse.Nay.Status.VENTER_PÅ_LEDIG_SAKSBEHANDLER

            avklaringsbehovSomTriggerUnderBehandling.any { it.harLøsning() } ->
                BehandlingHendelse.Nay.Status.UNDER_BEHANDLING

            else -> null
        }
        val aktivtVentepunkt = alleAvklaringsbehov
            .find { it.erVentepunkt() && !it.erAvsluttet() }
            ?.let { behov ->
                when (behov.venteårsak()) {
                    ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelse.Nay.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER
                    else -> null
                }
            }
        val sistEndret = if (progresjon == BehandlingHendelseProgresjon.IKKE_PÅBEGYNT) {
            null
        } else {
            alleAvklaringsbehov.filter { it.erAvsluttet() }.maxOfOrNull { it.sistEndret() }
        }

        return BehandlingHendelse.Nay(
            sistEndret = sistEndret,
            progresjon = progresjon,
            status = when (progresjon) {
                BehandlingHendelseProgresjon.AKTIV -> aktivtVentepunkt ?: status
                else -> null
            }
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
            sistEndret = ferdigBehandletAvklaringsbehov.filter { it.erAvsluttet() }.maxOfOrNull { it.sistEndret() },
            progresjon = progresjon,
        )
    }

    fun utledBehandlingVisningHendelser(behandling: Behandling): List<BehandlingHendelse> {
        val hendelser = listOf(
            hendelseSøknadMottatt(behandling),
            hendelseAlderLovvalgMedlemskap(behandling),
            hendelseNavKontor(behandling),
            hendelseNay(behandling),
            hendelseFerdigBehandlet(behandling)
        )
        return fjernForbigåtteHendelser(avstemHendelser(hendelser))
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

    private fun avstemHendelser(hendelser: List<BehandlingHendelse>): List<BehandlingHendelse> {
        val ferdigBehandlet = hendelser.any {
            it is BehandlingHendelse.FerdigBehandlet && it.progresjon == BehandlingHendelseProgresjon.FULLFØRT
        }
        val aktivIndeks = if (ferdigBehandlet) {
            -1
        } else {
            hendelser.indexOfLast { it.progresjon == BehandlingHendelseProgresjon.AKTIV }
        }
        return hendelser.mapIndexed { indeks, hendelse ->
            if (indeks == aktivIndeks) hendelse else hendelse.somInaktiv()
        }
    }

    private fun fjernForbigåtteHendelser(hendelser: List<BehandlingHendelse>): List<BehandlingHendelse> {
        val sistePåbegynt = hendelser.indexOfLast { it.progresjon != BehandlingHendelseProgresjon.IKKE_PÅBEGYNT }
        return hendelser.filterIndexed { indeks, hendelse ->
            hendelse.progresjon != BehandlingHendelseProgresjon.IKKE_PÅBEGYNT || indeks > sistePåbegynt
        }
    }

    private fun BehandlingHendelse.somInaktiv(): BehandlingHendelse {
        val nyProgresjon = when (progresjon) {
            BehandlingHendelseProgresjon.AKTIV -> BehandlingHendelseProgresjon.FULLFØRT
            else -> progresjon
        }
        return when (this) {
            is BehandlingHendelse.SøknadMottatt -> copy(progresjon = nyProgresjon)
            is BehandlingHendelse.AlderLovvalgMedlemskap -> copy(progresjon = nyProgresjon, status = null)
            is BehandlingHendelse.NavKontor -> copy(progresjon = nyProgresjon, status = null)
            is BehandlingHendelse.Nay -> copy(progresjon = nyProgresjon, status = null)
            is BehandlingHendelse.FerdigBehandlet -> copy(progresjon = nyProgresjon)
        }
    }
}