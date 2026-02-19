package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.komponenter.verdityper.Bruker
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
        return if (brevbestilling.typeBrev.erAutomatiskBrev()) {
            emptyList()
        } else if (brevbestilling.typeBrev.erVedtak()) {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(brevbestilling.behandlingId)
            val signaturBeslutter = utledSignatur(Rolle.BESLUTTER, avklaringsbehovene)
            val signaturSaksbehandlerNasjonal = utledSignatur(Rolle.SAKSBEHANDLER_NASJONAL, avklaringsbehovene)
            val signaturKvalitetssikrer = utledSignatur(Rolle.KVALITETSSIKRER, avklaringsbehovene)
            val signaturSaksbehandlerOppfolging = utledSignatur(Rolle.SAKSBEHANDLER_OPPFOLGING, avklaringsbehovene)

            buildList {
                signaturSaksbehandlerNasjonal?.let { add(it) }
                signaturKvalitetssikrer?.let { add(it) }
                signaturSaksbehandlerOppfolging?.let { add(it) }
                if (signaturBeslutter == null && none { it.navIdent == bruker.ident }) {
                    // Dersom ingen har saksbehandlet med rollen beslutter så tas innlogget bruker med i signatur.
                    // Dette fordi det er beslutter som skriver vedtaksbrev.
                    addFirst(
                        SignaturGrunnlag(
                            navIdent = bruker.ident,
                            rolle = null
                        )
                    )
                } else {
                    signaturBeslutter?.let { addFirst(it) }
                }
            }.distinctBy { it.navIdent }
        } else {
            listOf(SignaturGrunnlag(bruker.ident, null))
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
            .filterNot {
                listOf(
                    Definisjon.MANUELT_SATT_PÅ_VENT,
                    Definisjon.VURDER_TREKK_AV_SØKNAD
                ).contains(it)
            }
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
