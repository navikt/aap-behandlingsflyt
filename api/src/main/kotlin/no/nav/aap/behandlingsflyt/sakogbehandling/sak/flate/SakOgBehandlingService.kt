package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.repository.RepositoryProvider
import java.util.*

class SakOgBehandlingService(private val repositoryProvider: RepositoryProvider) {

    fun finnSakOgBehandlinger(saksnummer: Saksnummer): SakOgBehandlinger {
        var søknadErTrukket: Boolean? = null
        val resultatUtleder = ResultatUtleder(repositoryProvider)
        val sak = repositoryProvider.provide<SakRepository>().hent(saksnummer)

        val behandlinger =
            repositoryProvider.provide<BehandlingRepository>().hentAlleFor(sak.id).map { behandling ->
                if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
                    søknadErTrukket =
                        resultatUtleder.utledResultatFørstegangsBehandling(behandling) == Resultat.TRUKKET
                }
                val vurderingsbehov = behandling.vurderingsbehov().map(VurderingsbehovMedPeriode::type)
                BehandlinginfoDTO(
                    referanse = behandling.referanse.referanse,
                    type = behandling.typeBehandling().identifikator(),
                    status = behandling.status(),
                    vurderingsbehov = vurderingsbehov,
                    årsakTilOpprettelse = behandling.årsakTilOpprettelse,
                    opprettet = behandling.opprettetTidspunkt,
                    eksternSaksbehandlingsløsningUrl = null,
                )
            }

        val tilbakekrevingsbehandlinger = repositoryProvider.provide<TilbakekrevingRepository>().hent(sak.id).map { tilbakekrevingBehandling ->
            val behandlingsrefString = tilbakekrevingBehandling.eksternBehandlingId ?: throw IllegalStateException("TilbakekrevingBehandling skal ha eksternBehandlingId")
            BehandlinginfoDTO(
                referanse = UUID.fromString(behandlingsrefString),
                type = "Tilbakekreving",
                status = when (tilbakekrevingBehandling.behandlingsstatus) {
                    TilbakekrevingBehandlingsstatus.OPPRETTET -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.OPPRETTET
                    TilbakekrevingBehandlingsstatus.TIL_BEHANDLING -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES
                    TilbakekrevingBehandlingsstatus.TIL_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES
                    TilbakekrevingBehandlingsstatus.RETUR_FRA_BESLUTTER -> no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES
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
