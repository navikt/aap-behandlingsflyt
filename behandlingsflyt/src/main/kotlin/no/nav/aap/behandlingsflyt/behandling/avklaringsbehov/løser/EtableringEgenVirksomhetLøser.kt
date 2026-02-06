package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class EtableringEgenVirksomhetLøser(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository
) : AvklaringsbehovsLøser<EtableringEgenVirksomhetLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: EtableringEgenVirksomhetLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        // Sjekk at første dag er minimum én dag etter påbegynt 11 5/11 6
        //  TODO: Må slå sammen her på en god måte
        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toEtableringEgenVirksomhetVurdering(kontekst) }
        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        etableringEgenVirksomhetRepository.lagre(
            behandlingId = behandling.id,
            etableringEgenvirksomhetVurderinger = gamleVurderinger + nyeVurderinger
        )
        return LøsningsResultat(begrunnelse = "Vurdert etablering egen virksomhet")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.ETABLERING_EGEN_VIRKSOMHET
    }
}