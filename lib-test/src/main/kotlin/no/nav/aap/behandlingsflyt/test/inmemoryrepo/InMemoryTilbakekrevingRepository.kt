package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.erAvsluttet
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.UUID

object InMemoryTilbakekrevingRepository : TilbakekrevingRepository {
    private val behandlinger = mutableMapOf<UUID, Pair<SakId, Tilbakekrevingsbehandling>>()

    override fun lagre(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        behandlinger[tilbakekrevingshendelse.tilbakekrevingBehandlingId] = sakId to Tilbakekrevingsbehandling(
            tilbakekrevingBehandlingId = tilbakekrevingshendelse.tilbakekrevingBehandlingId,
            eksternFagsakId = tilbakekrevingshendelse.eksternFagsakId,
            hendelseOpprettet = tilbakekrevingshendelse.hendelseOpprettet,
            eksternBehandlingId = tilbakekrevingshendelse.eksternBehandlingId,
            sakOpprettet = tilbakekrevingshendelse.sakOpprettet,
            varselSendt = tilbakekrevingshendelse.varselSendt,
            venteGrunn = tilbakekrevingshendelse.venteGrunn,
            gjenopptas = tilbakekrevingshendelse.gjenopptas,
            behandlingsstatus = tilbakekrevingshendelse.behandlingsstatus,
            totaltFeilutbetaltBeløp = tilbakekrevingshendelse.totaltFeilutbetaltBeløp,
            saksbehandlingURL = tilbakekrevingshendelse.tilbakekrevingSaksbehandlingUrl,
            fullstendigPeriode = tilbakekrevingshendelse.fullstendigPeriode,
            vedtaksdato = tilbakekrevingshendelse.vedtaksdato,
        )
    }

    override fun hent(sakId: SakId): List<Tilbakekrevingsbehandling> =
        behandlinger.values.filter { it.first == sakId }.map { it.second }

    override fun hent(tilbakekrevingsBehandlingId: UUID): Tilbakekrevingsbehandling =
        requireNotNull(behandlinger[tilbakekrevingsBehandlingId]).second

    override fun hentAvsluttaTilbakekrevingsBehandlinger(personId: SakId): List<Tilbakekrevingsbehandling> =
        hent(personId).filter { it.behandlingsstatus.erAvsluttet() }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = Unit

    override fun slett(behandlingId: BehandlingId) = Unit
}
