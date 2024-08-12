package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingRepository

class AvklarBarnetilleggLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBarnetilleggLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBarnetilleggLøsning): LøsningsResultat {

        val barnVurderingRepository = BarnVurderingRepository(connection)
        barnVurderingRepository.lagre(
            kontekst.kontekst.behandlingId,
            løsning.vurdering.barn.map { it.tilBarnVurderingPeriode() }.toSet()
        )

        return LøsningsResultat(begrunnelse = løsning.vurdering.barn.joinToString(System.lineSeparator()) { it.begrunnelse })
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BARNETILLEGG
    }
}
