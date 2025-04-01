package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.tilgang.Rolle as TilgangRolle

class SignaturService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) {
    fun finnSignaturGrunnlag(brevbestilling: Brevbestilling, bruker: Bruker): List<SignaturGrunnlag> {
        require(brevbestilling.status == Status.FORHÅNDSVISNING_KLAR) {
            "Kan ikke utlede signaturer på brev i status ${brevbestilling.status}"
        }

        return when (brevbestilling.typeBrev) {
            TypeBrev.VEDTAK_AVSLAG, TypeBrev.VEDTAK_INNVILGELSE, TypeBrev.VEDTAK_ENDRING -> {

                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(brevbestilling.behandlingId)
                listOfNotNull(
                    utledSignatur(Rolle.KVALITETSSIKRER, avklaringsbehovene),
                    utledSignatur(Rolle.SAKSBEHANDLER_OPPFOLGING, avklaringsbehovene),
                    utledSignatur(Rolle.BESLUTTER, avklaringsbehovene),
                    utledSignatur(Rolle.SAKSBEHANDLER_NASJONAL, avklaringsbehovene)
                )
            }

            TypeBrev.VARSEL_OM_BESTILLING -> {
                emptyList()
            }

            TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> {
                listOf(SignaturGrunnlag(bruker.ident, Rolle.SAKSBEHANDLER_OPPFOLGING))
            }
        }
    }

    private val rolleTilAvklaringsbehov: Map<Rolle, List<Definisjon>> = buildMap {
        put(Rolle.SAKSBEHANDLER_OPPFOLGING, definisjonerSomLøsesAv(TilgangRolle.SAKSBEHANDLER_OPPFOLGING))
        put(Rolle.SAKSBEHANDLER_NASJONAL, definisjonerSomLøsesAv(TilgangRolle.SAKSBEHANDLER_NASJONAL))
        put(Rolle.KVALITETSSIKRER, definisjonerSomLøsesAv(TilgangRolle.KVALITETSSIKRER))
        put(Rolle.BESLUTTER, definisjonerSomLøsesAv(TilgangRolle.BESLUTTER))
    }

    private fun definisjonerSomLøsesAv(rolle: TilgangRolle): List<Definisjon> {
        return Definisjon.entries.filter { it.løsesAv.contains(rolle) }
    }

    private fun utledSignatur(rolle: Rolle, avklaringsbehovene: Avklaringsbehovene): SignaturGrunnlag? {
        val definisjoner = rolleTilAvklaringsbehov.getValue(rolle)
        return avklaringsbehovene.hentBehovForDefinisjon(definisjoner)
            .filter { it.endretAv().erNavIdent() }
            .maxByOrNull { it.historikk.max().tidsstempel }
            ?.let {
                SignaturGrunnlag(it.endretAv(), rolle)
            }
    }
}

private fun String.erNavIdent(): Boolean {
    return this.matches(Regex("\\w\\d{6}"));
}
