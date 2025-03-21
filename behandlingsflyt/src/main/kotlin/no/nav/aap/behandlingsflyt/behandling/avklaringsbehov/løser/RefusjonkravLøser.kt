package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravLøser(val connection: DBConnection) : AvklaringsbehovsLøser<RefusjonkravLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: RefusjonkravLøsning): LøsningsResultat {
        refusjonkravRepository.lagre(kontekst.kontekst.sakId, kontekst.behandlingId(), løsning.refusjonkravVurdering)
        return LøsningsResultat("Vurdert refusjonskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.REFUSJON_KRAV
    }
}
