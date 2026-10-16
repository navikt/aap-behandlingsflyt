package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseNavn
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseStatus
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseType
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingVisningDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.ZoneId

class BehandlingVisningDatadelingService(
    private val trukketSøknadService: TrukketSøknadService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    fun utledBehandlingVisningHendelser(behandling: Behandling): List<BehandlingHendelseDto> {
        val søknadErTrukketVurdering = trukketSøknadService.søknadErTrukketVurdering(behandling.id)
        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        val vedtakFattetAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK)
        val ventehendelser = avklaringsbehovene.alle()
            .filter { it.erVentepunkt() && it.erIkkeAvbrutt() }
            .mapNotNull { behov ->
                val ventepunktHendelseNavn =
                    when(behov.venteårsak()) {
                        ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> BehandlingHendelseNavn.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
                        ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER -> BehandlingHendelseNavn.VENTER_PÅ_SVAR_FRA_BRUKER
                        ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL -> BehandlingHendelseNavn.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL
                        ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelseNavn.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
                        ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER -> BehandlingHendelseNavn.VENTER_PÅ_OPPLYSNINGER
                        else -> null
                    }
                ventepunktHendelseNavn?.let { navn ->
                    BehandlingHendelseDto(
                        navn = navn,
                        status = if (behov.erAvsluttet()) BehandlingHendelseStatus.FULLFØRT else BehandlingHendelseStatus.AKTIV,
                        tidsstempel = behov.sistEndret(),
                    )
                }
            }

        var hendelser = mutableListOf(
            BehandlingHendelseDto(
                navn = BehandlingHendelseNavn.SØKNAD_MOTTATT,
                tidsstempel = behandling.opprettetTidspunkt,
                status = BehandlingHendelseStatus.FULLFØRT
            )
        )
        hendelser.addAll(ventehendelser)
        hendelser.add(
            BehandlingHendelseDto(
                navn = BehandlingHendelseNavn.UNDER_BEHANDLING,
                // TODO: hva er riktig tidspunkt for 'under behandling'
                tidsstempel = behandling.opprettetTidspunkt,
                status = if (behandling.status().erAvsluttet()) {
                    BehandlingHendelseStatus.FULLFØRT
                } else {
                    BehandlingHendelseStatus.AKTIV
                }
            )
        )
        if (søknadErTrukketVurdering != null) {
            hendelser.add(
                BehandlingHendelseDto(
                    navn = BehandlingHendelseNavn.SØKNAD_TRUKKET,
                    tidsstempel = søknadErTrukketVurdering.vurdert.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                    status = BehandlingHendelseStatus.FULLFØRT
                )
            )
        } else if (behandling.status().erAvsluttet() && vedtakFattetAvklaringsbehov != null) {
            hendelser.add(
                BehandlingHendelseDto(
                    navn = BehandlingHendelseNavn.FERDIG_BEHANDLET,
                    tidsstempel = vedtakFattetAvklaringsbehov.sistAvsluttet(),
                    status = BehandlingHendelseStatus.FULLFØRT
                )
            )
        }

        return hendelser.sortedBy { it.tidsstempel }
    }

    fun utledBehandlingVisning(behandling: Behandling): BehandlingVisningDto? {
        if(behandling.erYtelsesbehandling() && behandling.årsakTilOpprettelse == ÅrsakTilOpprettelse.SØKNAD){
            return BehandlingVisningDto(
                behandlingReferanse = behandling.referanse,
                avsluttet = behandling.status().erAvsluttet(),
                behandlingType = BehandlingHendelseType.SØKNAD,
                hendelser = utledBehandlingVisningHendelser(behandling),
            )
        }
        return null
    }
}