package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.datadeling.BehandlingHendelse
import no.nav.aap.behandlingsflyt.datadeling.BehandlingHendelseNavn
import no.nav.aap.behandlingsflyt.datadeling.BehandlingHendelseStatus
import no.nav.aap.behandlingsflyt.datadeling.BehandlingHendelseType
import no.nav.aap.behandlingsflyt.datadeling.BehandlingVisning
import no.nav.aap.behandlingsflyt.datadeling.ventepunktTilehandlingHendelseNavn
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
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

    fun utledBehandlingVisningHendelser(behandling: Behandling): List<BehandlingHendelse> {
        val søknadErTrukketVurdering = trukketSøknadService.søknadErTrukketVurdering(behandling.id)
        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        val vedtakFattetAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK)
        val ventehendelser = avklaringsbehovene.alle()
            .filter { it.erVentepunkt() && it.erIkkeAvbrutt() }
            .mapNotNull { behov ->
                ventepunktTilehandlingHendelseNavn(behov)?.let { navn ->
                    BehandlingHendelse(
                        navn = navn,
                        status = if (behov.erAvsluttet()) BehandlingHendelseStatus.FULLFØRT else BehandlingHendelseStatus.AKTIV,
                        tidsstempel = behov.sistEndret(),
                    )
                }
            }

        var hendelser = mutableListOf(
            BehandlingHendelse(
                navn = BehandlingHendelseNavn.SØKNAD_MOTTATT,
                tidsstempel = behandling.opprettetTidspunkt,
                status = BehandlingHendelseStatus.FULLFØRT
            )
        )
        hendelser.addAll(ventehendelser)
        hendelser.add(
            BehandlingHendelse(
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
                BehandlingHendelse(
                    navn = BehandlingHendelseNavn.SØKNAD_TRUKKET,
                    tidsstempel = søknadErTrukketVurdering.vurdert.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
                    status = BehandlingHendelseStatus.FULLFØRT
                )
            )
        } else if (behandling.status().erAvsluttet() && vedtakFattetAvklaringsbehov != null) {
            hendelser.add(
                BehandlingHendelse(
                    navn = BehandlingHendelseNavn.FERDIG_BEHANDLET,
                    tidsstempel = vedtakFattetAvklaringsbehov.sistAvsluttet(),
                    status = BehandlingHendelseStatus.FULLFØRT
                )
            )
        }

        return hendelser.sortedBy { it.tidsstempel }
    }

    fun utledBehandlingVisning(behandling: Behandling): BehandlingVisning? {
        if(behandling.erYtelsesbehandling() && behandling.årsakTilOpprettelse == ÅrsakTilOpprettelse.SØKNAD){
            return BehandlingVisning(
                behandlingReferanse = behandling.referanse,
                avsluttet = behandling.status().erAvsluttet(),
                behandlingType = BehandlingHendelseType.SØKNAD,
                hendelser = utledBehandlingVisningHendelser(behandling),
            )
        }
        return null
    }
}