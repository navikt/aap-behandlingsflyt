package no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FatteVedtakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)

    override fun løs(kontekst: FlytKontekst, løsning: FatteVedtakLøsning): LøsningsResultat {
        løsning.vurderinger.forEach { vurdering ->
            run {
                //TODO: Kan denne blokka uttrykket med en metode på Avkarlingsbehovene?
                val avklaringsbehovene = avklaringsbehovRepository.hent(kontekst.behandlingId)

                val definisjon = Definisjon.forKode(vurdering.definisjon)
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

                if (avklaringsbehov == null) {
                    throw IllegalStateException("Fant ikke avklaringsbehov med $definisjon for behandling $kontekst.behandlingId")
                }
                if (!(avklaringsbehov.erTotrinn() && avklaringsbehov.erAvsluttet())) {
                    throw IllegalStateException("Har ikke rett tilstand på avklaringsbehov")
                }

                val status = if (vurdering.godkjent!!) {
                    Status.TOTRINNS_VURDERT
                } else {
                    Status.SENDT_TILBAKE_FRA_BESLUTTER
                }

                avklaringsbehovRepository.opprettAvklaringsbehovEndring(
                    avklaringsbehov.id,
                    status,
                    vurdering.begrunnelse!!,
                    "Saksbehandler"
                )
            }
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
