package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class FatteVedtakLøser(private val connection: DBConnection) : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val behandlingRepository = BehandlingRepositoryImpl(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FatteVedtakLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.kontekst.behandlingId)

        løsning.vurderinger.all { it.valider() }

        if (skalSendesTilbake(løsning.vurderinger)) {
            val flyt = utledType(behandling.typeBehandling()).flyt()
            val vurderingerSomErSendtTilbake = løsning.vurderinger
                .filter { it.godkjent == false }

            val tidligsteStegMedRetur = vurderingerSomErSendtTilbake
                .map { Definisjon.forKode(it.definisjon) }
                .map { it.løsesISteg }
                .minWith(flyt.compareable())

            val vurderingerFørRetur = løsning.vurderinger
                .filter { it.godkjent == true }
                .filter { flyt.erStegFør(Definisjon.forKode(it.definisjon).løsesISteg, tidligsteStegMedRetur) }

            val vurderingerSomMåReåpnes = avklaringsbehovene.alle()
                .filterNot { behov ->
                    behov.definisjon in setOf(
                        Definisjon.FORESLÅ_VEDTAK,
                        Definisjon.FATTE_VEDTAK,
                        Definisjon.KVALITETSSIKRING
                    )
                }
                .filter { vurdering ->
                    vurderingerSomErSendtTilbake.none { it.definisjon == vurdering.definisjon.kode } &&
                            vurderingerFørRetur.none { it.definisjon == vurdering.definisjon.kode }
                }

            vurderingerFørRetur.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    godkjent = vurdering.godkjent!!,
                    begrunnelse = vurdering.begrunnelse(),
                    vurdertAv = kontekst.bruker.ident
                )
            }

            vurderingerSomErSendtTilbake.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    begrunnelse = vurdering.begrunnelse(),
                    godkjent = vurdering.godkjent!!,
                    årsakTilRetur = vurdering.grunner ?: listOf(),
                    vurdertAv = kontekst.bruker.ident // TODO: Hente fra context
                )
            }

            vurderingerSomMåReåpnes.forEach { vurdering ->
                avklaringsbehovene.reåpne(definisjon = vurdering.definisjon)
            }
        } else {
            løsning.vurderinger.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    godkjent = vurdering.godkjent!!,
                    begrunnelse = vurdering.begrunnelse(),
                    vurdertAv = kontekst.bruker.ident // TODO: Hente fra context
                )
            }
        }
        val sammenstiltBegrunnelse = sammenstillBegrunnelse(løsning)

        return LøsningsResultat(sammenstiltBegrunnelse)
    }

    private fun skalSendesTilbake(vurderinger: List<TotrinnsVurdering>): Boolean {
        return vurderinger.any { it.godkjent == false }
    }

    private fun sammenstillBegrunnelse(løsning: FatteVedtakLøsning): String {
        return løsning.vurderinger.joinToString("\\n") { it.begrunnelse() }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
