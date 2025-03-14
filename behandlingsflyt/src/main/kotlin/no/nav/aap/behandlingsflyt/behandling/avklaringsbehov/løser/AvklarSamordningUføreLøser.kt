package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningUføreLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSamordningUføreLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val samordningUføreRepository = repositoryProvider.provide<SamordningUføreRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningUføreLøsning
    ): LøsningsResultat {
        samordningUføreRepository.lagre(kontekst.behandlingId(), løsning.samordningUføreVurdering)
        return LøsningsResultat("Vurdert samordning uføre")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_UFØRE
    }
}