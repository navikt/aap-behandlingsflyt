package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.erAvsluttet
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object InMemoryTilbakekrevingRepository : TilbakekrevingRepository {
    private data class LagretBehandling(val sakId: SakId, val behandling: Tilbakekrevingsbehandling)

    private val behandlinger = ConcurrentHashMap<UUID, LagretBehandling>()

    override fun lagre(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        behandlinger.compute(tilbakekrevingshendelse.tilbakekrevingBehandlingId) { _, eksisterende ->
            val hendelse = tilbakekrevingshendelse
            LagretBehandling(
                eksisterende?.sakId ?: sakId,
                Tilbakekrevingsbehandling(
                    tilbakekrevingBehandlingId = hendelse.tilbakekrevingBehandlingId,
                    eksternFagsakId = eksisterende?.behandling?.eksternFagsakId ?: hendelse.eksternFagsakId,
                    hendelseOpprettet = hendelse.hendelseOpprettet,
                    eksternBehandlingId = hendelse.eksternBehandlingId,
                    sakOpprettet = eksisterende?.behandling?.sakOpprettet ?: hendelse.sakOpprettet,
                    varselSendt = hendelse.varselSendt,
                    venteGrunn = hendelse.venteGrunn,
                    gjenopptas = hendelse.gjenopptas,
                    behandlingsstatus = hendelse.behandlingsstatus,
                    totaltFeilutbetaltBeløp = hendelse.totaltFeilutbetaltBeløp,
                    saksbehandlingURL = hendelse.tilbakekrevingSaksbehandlingUrl,
                    fullstendigPeriode = hendelse.fullstendigPeriode,
                    vedtaksdato = hendelse.vedtaksdato
                )
            )
        }
    }

    override fun hent(sakId: SakId): List<Tilbakekrevingsbehandling> =
        behandlinger.values.filter { it.sakId == sakId }.map { it.behandling }

    override fun hent(tilbakekrevingsBehandlingId: UUID): Tilbakekrevingsbehandling =
        behandlinger.getValue(tilbakekrevingsBehandlingId).behandling

    override fun hentAvsluttaTilbakekrevingsBehandlinger(sakId: SakId): List<Tilbakekrevingsbehandling> =
        hent(sakId).filter { it.behandlingsstatus.erAvsluttet() }.sortedByDescending { it.sakOpprettet }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Tilbakekrevinger er knyttet til sak, ikke til Kelvin-behandling.
    }

    override fun slett(behandlingId: BehandlingId) {
        // Tilbakekrevinger er knyttet til sak, ikke til Kelvin-behandling.
    }
}
