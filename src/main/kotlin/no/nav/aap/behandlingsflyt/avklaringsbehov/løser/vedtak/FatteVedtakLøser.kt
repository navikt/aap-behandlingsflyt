package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FatteVedtakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val behandlingRepository = behandlingRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: FatteVedtakLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.behandlingId)

        lateinit var sammenstiltBegrunnelse: String
        if (skalSendesTilbake(løsning.vurderinger)) {
            val flyt = behandling.flyt()
            val vurderingerSomErSendtTilbake = løsning.vurderinger
                .filter { it.godkjent == false }

            val tidligsteStegMedRetur = vurderingerSomErSendtTilbake
                .map { Definisjon.forKode(it.definisjon) }
                .map { it.løsesISteg }
                .minWith(flyt.compareable())

            val vurderingerFørRetur = løsning.vurderinger
                .filter { it.godkjent == true }
                .filter { flyt.erStegFør(Definisjon.forKode(it.definisjon).løsesISteg, tidligsteStegMedRetur) }

            val vurderingerSomMåReåpnes = løsning.vurderinger
                .filter { vurdering ->
                    vurderingerSomErSendtTilbake.none { it.definisjon == vurdering.definisjon } &&
                            vurderingerFørRetur.none { it.definisjon == vurdering.definisjon }
                }

            vurderingerFørRetur.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    begrunnelse = vurdering.begrunnelse!!,
                    godkjent = vurdering.godkjent!!,
                    vurdertAv = "saksbehandler" // TODO: Hente fra context
                )
            }

            vurderingerSomErSendtTilbake.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    begrunnelse = vurdering.begrunnelse!!,
                    godkjent = vurdering.godkjent!!,
                    vurdertAv = "saksbehandler" // TODO: Hente fra context
                )
            }

            vurderingerSomMåReåpnes.forEach { vurdering ->
                avklaringsbehovene.reåpne(definisjon = Definisjon.forKode(vurdering.definisjon))
            }
        } else {
            løsning.vurderinger.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    begrunnelse = vurdering.begrunnelse!!,
                    godkjent = vurdering.godkjent!!,
                    vurdertAv = "saksbehandler" // TODO: Hente fra context
                )
            }
        }
        sammenstiltBegrunnelse = sammenstillBegrunnelse(løsning)

        return LøsningsResultat(sammenstiltBegrunnelse)
    }

    private fun skalSendesTilbake(vurderinger: List<TotrinnsVurdering>): Boolean {
        return vurderinger.any { it.godkjent == false }
    }

    private fun sammenstillBegrunnelse(løsning: FatteVedtakLøsning): String {
        return løsning.vurderinger.joinToString("\\n") { it.begrunnelse!! }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
