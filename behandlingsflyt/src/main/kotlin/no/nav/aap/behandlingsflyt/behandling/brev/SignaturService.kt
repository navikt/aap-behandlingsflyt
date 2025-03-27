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
                    utledSignatur(Rolle.SAKSBEHANDLER_OPPFOLGING, avklaringsbehovene),
                    utledSignatur(Rolle.SAKSBEHANDLER_NASJONAL, avklaringsbehovene),
                    utledSignatur(Rolle.KVALITETSSIKRER, avklaringsbehovene),
                    utledSignatur(Rolle.BESLUTTER, avklaringsbehovene)
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

    private val rolleTilAvklaringsbehov = buildMap {
        // TODO finne en måte å hente siste veileder som har løst avklaringsbehov på behandlingen
        put(Rolle.SAKSBEHANDLER_OPPFOLGING, Definisjon.AVKLAR_SYKDOM)
        put(Rolle.SAKSBEHANDLER_NASJONAL, Definisjon.KVALITETSSIKRING)
        put(Rolle.KVALITETSSIKRER, Definisjon.FORESLÅ_VEDTAK)
        put(Rolle.BESLUTTER, Definisjon.FATTE_VEDTAK)
    }

    private fun utledSignatur(rolle: Rolle, avklaringsbehovene: Avklaringsbehovene): SignaturGrunnlag? {
        val definisjon = rolleTilAvklaringsbehov.getValue(rolle)
        return avklaringsbehovene.hentBehovForDefinisjon(definisjon)?.let {
            SignaturGrunnlag(it.endretAv(), rolle)
        }
    }
}
