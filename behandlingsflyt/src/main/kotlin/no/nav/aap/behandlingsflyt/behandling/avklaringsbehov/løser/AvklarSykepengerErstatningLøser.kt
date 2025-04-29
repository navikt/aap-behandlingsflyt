package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

class AvklarSykepengerErstatningLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sykepengerErstatningRepository = repositoryProvider.provide<SykepengerErstatningRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSykepengerErstatningLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        sykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.sykepengeerstatningVurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykepengeerstatningVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
