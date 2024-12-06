package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.repository.RepositoryFactory

class AvklarSykepengerErstatningLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    private val repositoryFactory = RepositoryFactory(connection)
    private val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
    private val sykepengerErstatningRepository = SykepengerErstatningRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSykepengerErstatningLøsning): LøsningsResultat {
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
