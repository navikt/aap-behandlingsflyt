package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

class VurderFormkravLøser(val connection: DBConnection) : AvklaringsbehovsLøser<VurderFormkravLøsning> {
    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val formkravRepository = repositoryProvider.provide<FormkravRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderFormkravLøsning): LøsningsResultat {
        formkravRepository.lagre(
            kontekst.kontekst.behandlingId,
            formkravVurdering = løsning.formkravVurdering.tilVurdering(kontekst.bruker)
        )
        return LøsningsResultat(
            begrunnelse = løsning.formkravVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_FORMKRAV
    }
}