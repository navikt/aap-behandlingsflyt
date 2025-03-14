package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarBistandLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val bistandRepository = repositoryProvider.provide<BistandRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarBistandsbehovLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val bistandsVurdering = løsning.bistandsVurdering.tilBistandVurdering(kontekst.bruker)
        bistandRepository.lagre(
            behandlingId = behandling.id,
            bistandVurdering = bistandsVurdering
        )

        return LøsningsResultat(
            begrunnelse = bistandsVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
