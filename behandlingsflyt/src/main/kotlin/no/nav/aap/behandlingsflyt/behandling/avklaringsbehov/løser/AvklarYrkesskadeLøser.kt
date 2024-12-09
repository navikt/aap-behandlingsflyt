package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.repository.RepositoryProvider

class AvklarYrkesskadeLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
    private val sykdomRepository = SykdomRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = løsning.yrkesskadesvurdering.toYrkesskadevurdering(),
        )

        return LøsningsResultat(
            begrunnelse = løsning.yrkesskadesvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_YRKESSKADE
    }
}
