package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.Rolle
import kotlin.collections.filter

class MellomlagretVurderingService(
    private val mellomlagretVurderingRepository: MellomlagretVurderingRepository,
    private val behandlingRepository: BehandlingRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        mellomlagretVurderingRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    fun hentMellomlagredeVurderingerFørSteg(
        behandling: Behandling,
        steg: StegType,
        løsesAv: List<Rolle>
    ): List<MellomlagretVurdering> {
        val foregåendeSteg = behandling.flyt().stegene().takeWhile { it != steg }
        val relevanteAvklaringsbehov =
            Definisjon.entries
                .filter { behov -> foregåendeSteg.contains(behov.løsesISteg) }
                .filter { it.løsesAv.any { rolle -> løsesAv.contains(rolle) } }

        return mellomlagretVurderingRepository.hentForAvklaringsbehov(
            behandling.id,
            relevanteAvklaringsbehov.map { it.kode })
    }

    fun hentMellomlagredeVurderingerFørSteg(
        behandlingsreferanse: BehandlingReferanse,
        steg: StegType,
        løsesAv: List<Rolle>
    ): List<MellomlagretVurdering> {
        val behandling = behandlingRepository.hent(behandlingsreferanse)
        return hentMellomlagredeVurderingerFørSteg(behandling, steg, løsesAv)
    }

    fun hentMellomlagredeVurderingerFørSteg(
        behandlingId: BehandlingId,
        steg: StegType,
        løsesAv: List<Rolle>
    ): List<MellomlagretVurdering> {
        val behandling = behandlingRepository.hent(behandlingId)
        return hentMellomlagredeVurderingerFørSteg(behandling, steg, løsesAv)
    }
}