package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider

class SakOgBehandlingService(
    private val resultatUtleder: ResultatUtleder,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val behandlingService: BehandlingService,
    private val personRepository: PersonRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        resultatUtleder = ResultatUtleder(repositoryProvider, gatewayProvider),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        tilbakekrevingRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        personRepository = repositoryProvider.provide(),
    )

    fun finnSaksinfo(ident: Ident): List<SaksinfoDTO> {
        val person = personRepository.finn(ident) ?: return emptyList()

        return sakRepository.finnSakerFor(person).map { sak ->
            val gjeldendeBehandling = behandlingRepository.finnGjeldendeVedtattBehandlingForSak(sak.id)
                ?.let { behandlingRepository.hent(it.behandlingId) }

            val resultat = if (gjeldendeBehandling == null) {
                behandlingRepository.hentAlleFor(sak.id)
                    .filter { it.erYtelsesbehandling() }
                    .maxByOrNull { it.opprettetTidspunkt }
                    ?.let {
                        resultatUtleder.utledResultat(it).takeIf { res -> res == Resultat.TRUKKET }
                    }
            } else {
                resultatUtleder.utledResultat(gjeldendeBehandling)
            }

            SaksinfoDTO(
                saksnummer = sak.saksnummer.toString(),
                opprettetTidspunkt = sak.opprettetTidspunkt,
                periode = sak.rettighetsperiode,
                ident = sak.person.aktivIdent().identifikator,
                resultat = when (resultat) {
                    Resultat.INNVILGELSE -> ResultatKode.INNVILGET
                    Resultat.AVSLAG -> ResultatKode.AVSLAG
                    Resultat.TRUKKET -> ResultatKode.TRUKKET
                    Resultat.AVBRUTT -> ResultatKode.AVBRUTT
                    null -> null
                }
            )
        }
    }

    fun finnSakOgBehandlinger(saksnummer: Saksnummer): SakOgBehandlinger {
        var søknadErTrukket: Boolean? = null
        val sak = sakRepository.hent(saksnummer)

        val behandlinger =
            behandlingRepository.hentAlleFor(sak.id).map { behandling ->
                if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
                    søknadErTrukket =
                        resultatUtleder.utledResultatFørstegangsBehandling(behandling) == Resultat.TRUKKET
                }
                val vurderingsbehov = behandling.vurderingsbehov().map(VurderingsbehovMedPeriode::type)
                BehandlinginfoDTO(
                    referanse = behandling.referanse.referanse,
                    typeBehandling = behandlingService.utledFaktiskBehandlingstype(behandling),
                    status = behandling.status(),
                    vurderingsbehov = vurderingsbehov,
                    årsakTilOpprettelse = behandling.årsakTilOpprettelse,
                    opprettet = behandling.opprettetTidspunkt,
                    eksternSaksbehandlingsløsningUrl = null,
                )
            }

        val tilbakekrevingsbehandlinger = tilbakekrevingRepository.hent(sak.id).map { tilbakekrevingBehandling ->
            BehandlinginfoDTO(
                referanse = tilbakekrevingBehandling.tilbakekrevingBehandlingId,
                typeBehandling = TypeBehandling.Tilbakekreving,
                status = when (tilbakekrevingBehandling.behandlingsstatus) {
                    TilbakekrevingBehandlingsstatus.OPPRETTET -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.OPPRETTET
                    TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES
                    TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES
                    TilbakekrevingBehandlingsstatus.TIL_GODKJENNING -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES
                    TilbakekrevingBehandlingsstatus.AVSLUTTET -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
                },
                vurderingsbehov = emptyList(),
                årsakTilOpprettelse = ÅrsakTilOpprettelse.TILBAKEKREVING_HENDELSE,
                opprettet = tilbakekrevingBehandling.sakOpprettet,
                eksternSaksbehandlingsløsningUrl = tilbakekrevingBehandling.saksbehandlingURL.toString(),
            )
        }

        return SakOgBehandlinger(
            sak = sak,
            behandlinger = (behandlinger + tilbakekrevingsbehandlinger).sortedByDescending { it.opprettet },
            søknadErTrukket = søknadErTrukket

        )
    }

}

data class SakOgBehandlinger(
    val sak: Sak,
    val behandlinger: List<BehandlinginfoDTO>,
    val søknadErTrukket: Boolean?
)
