package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.Rolle
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus
import no.nav.aap.brev.kontrakt.Rolle as SignaturRolle

class SignaturService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    fun finnSignaturGrunnlag(brevbestilling: Brevbestilling, bruker: Bruker): List<SignaturGrunnlag> {
        require(brevbestilling.status == Status.FORHÅNDSVISNING_KLAR) {
            "Kan ikke utlede signaturer på brev i status ${brevbestilling.status}"
        }

        return when (brevbestilling.typeBrev) {
            TypeBrev.VEDTAK_AVSLAG, TypeBrev.VEDTAK_INNVILGELSE, TypeBrev.VEDTAK_ENDRING,
            TypeBrev.KLAGE_AVVIST, TypeBrev.KLAGE_OPPRETTHOLDELSE, TypeBrev.KLAGE_TRUKKET -> {

                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(brevbestilling.behandlingId)
                listOfNotNull(
                    utledSignatur(Rolle.BESLUTTER, avklaringsbehovene),
                    utledSignatur(Rolle.SAKSBEHANDLER_NASJONAL, avklaringsbehovene),
                    utledSignatur(Rolle.KVALITETSSIKRER, avklaringsbehovene),
                    utledSignatur(Rolle.SAKSBEHANDLER_OPPFOLGING, avklaringsbehovene)
                )
            }

            TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT, TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV -> {
                listOf(SignaturGrunnlag(bruker.ident, null))
            }

            // Automatiske brev
            TypeBrev.VARSEL_OM_BESTILLING, TypeBrev.FORVALTNINGSMELDING -> emptyList()
        }
    }

    private val rolleTilAvklaringsbehov: Map<Rolle, List<Definisjon>> = buildMap {
        put(Rolle.SAKSBEHANDLER_OPPFOLGING, definisjonerSomLøsesAv(Rolle.SAKSBEHANDLER_OPPFOLGING))
        put(Rolle.SAKSBEHANDLER_NASJONAL, definisjonerSomLøsesAv(Rolle.SAKSBEHANDLER_NASJONAL))
        put(Rolle.KVALITETSSIKRER, listOf(Definisjon.KVALITETSSIKRING))
        put(Rolle.BESLUTTER, listOf(Definisjon.FATTE_VEDTAK))
    }

    private fun definisjonerSomLøsesAv(rolle: Rolle): List<Definisjon> {
        return Definisjon.entries.filter { it.løsesAv.contains(rolle) }
    }

    private fun utledSignatur(rolle: Rolle, avklaringsbehovene: Avklaringsbehovene): SignaturGrunnlag? {
        val definisjoner = rolleTilAvklaringsbehov.getValue(rolle)
        return avklaringsbehovene.hentBehovForDefinisjon(definisjoner).mapNotNull {
            it.historikk.filter { it.endretAv.erNavIdent() && it.status == AvklaringsbehovStatus.AVSLUTTET }.maxOrNull()
        }.maxOrNull()?.let {
            SignaturGrunnlag(it.endretAv, mapRolle(rolle))
        }
    }

    private fun mapRolle(rolle: Rolle): SignaturRolle? {
        return when (rolle) {
            Rolle.SAKSBEHANDLER_OPPFOLGING -> SignaturRolle.SAKSBEHANDLER_OPPFOLGING
            Rolle.SAKSBEHANDLER_NASJONAL -> SignaturRolle.SAKSBEHANDLER_NASJONAL
            Rolle.KVALITETSSIKRER -> SignaturRolle.KVALITETSSIKRER
            Rolle.BESLUTTER -> SignaturRolle.BESLUTTER
            else -> null
        }
    }
}

private fun String.erNavIdent(): Boolean {
    return this.matches(Regex("\\w\\d{6}"))
}
