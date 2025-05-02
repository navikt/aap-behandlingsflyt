package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.Effektuer11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Vurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

class Effektuer11_7Løser(
    connection: DBConnection,
) : AvklaringsbehovsLøser<Effektuer11_7Løsning> {
    private val effektuer117repository =
        RepositoryRegistry.provider(connection).provide<Effektuer11_7Repository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: Effektuer11_7Løsning
    ): LøsningsResultat {
        val vurdering = Effektuer11_7Vurdering(løsning.begrunnelse)
        effektuer117repository.lagreVurdering(
            kontekst.behandlingId(),
            vurdering = vurdering
        )
        return LøsningsResultat(begrunnelse = vurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.EFFEKTUER_11_7
    }
}