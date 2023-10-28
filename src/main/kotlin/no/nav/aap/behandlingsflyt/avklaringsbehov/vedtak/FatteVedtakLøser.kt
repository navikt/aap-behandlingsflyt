package no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FatteVedtakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    private val avklaringsbehovRepository = AvklaringsbehovRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: FatteVedtakLøsning): LøsningsResultat {
        løsning.vurderinger.forEach { vurdering ->
            avklaringsbehovRepository.toTrinnsVurdering(
                behandlingId = kontekst.behandlingId,
                definisjon = Definisjon.forKode(vurdering.definisjon),
                begrunnelse = vurdering.begrunnelse!!,
                godkjent = vurdering.godkjent!!
            )
        }

        return LøsningsResultat(sammenstillBegrunnelse(løsning))
    }

    private fun sammenstillBegrunnelse(løsning: FatteVedtakLøsning): String {
        return løsning.vurderinger.joinToString("\\n") { it.begrunnelse!! }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
