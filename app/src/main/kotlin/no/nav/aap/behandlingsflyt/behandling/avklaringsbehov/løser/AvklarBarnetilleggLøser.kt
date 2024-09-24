package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarBarnetilleggLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBarnetilleggLøsning> {

    private val barnRepository = BarnRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBarnetilleggLøsning): LøsningsResultat {

        barnRepository.lagreVurderinger(kontekst.kontekst.behandlingId, løsning.vurderingerForBarnetillegg.vurderteBarn)

        return LøsningsResultat(begrunnelse = "Vurdert barnetillegg")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BARNETILLEGG
    }
}
