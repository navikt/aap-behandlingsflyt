package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.repository.RepositoryFactory

class AvklarSykdomLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    private val repositoryFactory = RepositoryFactory(connection)
    private val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
    private val sykdomRepository = SykdomRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            sykdomsvurdering = løsning.sykdomsvurdering.toSykdomsvurdering(),
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykdomsvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
