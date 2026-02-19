package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.oppgave.enhet.OppgaveEnhetDto
import no.nav.aap.tilgang.Rolle
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus
import no.nav.aap.brev.kontrakt.Rolle as SignaturRolle

class SignaturService(
    private val unleashGateway: UnleashGateway,
    private val oppgavestyringGateway: OppgavestyringGateway,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) {
    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ) : this(
        unleashGateway = gatewayProvider.provide(),
        oppgavestyringGateway = gatewayProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide()
    )

    fun finnSignaturGrunnlag(brevbestilling: Brevbestilling, bruker: Bruker): List<SignaturGrunnlag> {
        require(brevbestilling.status == Status.FORHÅNDSVISNING_KLAR) {
            "Kan ikke utlede signaturer på brev i status ${brevbestilling.status}"
        }
        return if (unleashGateway.isEnabled(BehandlingsflytFeature.SignaturEnhetFraOppgave)) {
            finnSignaturGrunnlagV2(brevbestilling, bruker)
        } else {
            finnSignaturGrunnlagV1(brevbestilling, bruker)
        }
    }

    fun finnSignaturGrunnlagV1(brevbestilling: Brevbestilling, bruker: Bruker): List<SignaturGrunnlag> {
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
                            rolle = null,
                        enhet = null
                        )
                    )
                } else {
                    signaturBeslutter?.let { addFirst(it) }
                }
            }.distinctBy { it.navIdent }
        } else {
            listOf(SignaturGrunnlag(navIdent = bruker.ident, rolle = null, enhet = null))
        }
    }

    fun finnSignaturGrunnlagV2(brevbestilling: Brevbestilling, innloggetBruker: Bruker): List<SignaturGrunnlag> {
        return if (brevbestilling.typeBrev.erAutomatiskBrev()) {
            emptyList()
        } else if (brevbestilling.typeBrev.erVedtak()) {
            utledSignaturerForVedtak(brevbestilling, innloggetBruker)
        } else {
            listOf(utledSignaturMedInnloggetBruker(brevbestilling, innloggetBruker))
        }
    }

    private val rolleTilAvklaringsbehov: Map<Rolle, List<Definisjon>> = buildMap {
        put(Rolle.SAKSBEHANDLER_OPPFOLGING, definisjonerSomLøsesAv(Rolle.SAKSBEHANDLER_OPPFOLGING))
        put(Rolle.SAKSBEHANDLER_NASJONAL, definisjonerSomLøsesAv(Rolle.SAKSBEHANDLER_NASJONAL))
        put(Rolle.KVALITETSSIKRER, listOf(Definisjon.KVALITETSSIKRING))
        put(Rolle.BESLUTTER, listOf(Definisjon.FATTE_VEDTAK, Definisjon.SKRIV_VEDTAKSBREV))
    }

    /**
     * Roller kan ha overlappende definisjoner siden en definisjon kan løses av flere roller. Derfor er noen
     * definisjoner filtrert ut der det har ført til feil i signaturer.
     */
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
            SignaturGrunnlag(navIdent = it.endretAv, rolle = mapRolle(rolle), enhet = null)
        }
    }

    private fun utledSignaturerForVedtak(
        brevbestilling: Brevbestilling,
        innloggetBruker: Bruker
    ): List<SignaturGrunnlag> {
        val behandling = behandlingRepository.hent(brevbestilling.behandlingId)
        val oppgaveEnhetListe = oppgavestyringGateway.hentOppgaveEnhet(behandling.referanse).oppgaver
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(brevbestilling.behandlingId)

        return listOfNotNull(
            utledSignaturV2(Rolle.BESLUTTER, avklaringsbehovene, oppgaveEnhetListe, innloggetBruker),
            utledSignaturV2(Rolle.SAKSBEHANDLER_NASJONAL, avklaringsbehovene, oppgaveEnhetListe, innloggetBruker),
            utledSignaturV2(Rolle.KVALITETSSIKRER, avklaringsbehovene, oppgaveEnhetListe, innloggetBruker),
            utledSignaturV2(Rolle.SAKSBEHANDLER_OPPFOLGING, avklaringsbehovene, oppgaveEnhetListe, innloggetBruker),
        )
            .groupingBy { it.navIdent }
            .reduce { _, s1, s2 -> if (s1.harLøstAvklaringsbehov) s1 else s2 }
            .map { it.value.tilGrunnlag() }
            .sortedWith(signaturComparator)
    }

    private val signaturComparator: Comparator<SignaturGrunnlag> by lazy {
        val rekkefølge = mapOf(
            SignaturRolle.BESLUTTER to 0,
            SignaturRolle.SAKSBEHANDLER_NASJONAL to 1,
            SignaturRolle.KVALITETSSIKRER to 2,
            SignaturRolle.SAKSBEHANDLER_OPPFOLGING to 3
        )
        compareBy { signaturGrunnlag -> signaturGrunnlag.rolle?.let { rekkefølge[it] } ?: rekkefølge.size }
    }

    private fun utledSignaturMedInnloggetBruker(
        brevbestilling: Brevbestilling,
        innloggetBruker: Bruker
    ): SignaturGrunnlag {
        val behandling = behandlingRepository.hent(brevbestilling.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(brevbestilling.behandlingId)
        val avklaringsbehov = avklaringsbehovene.åpne().sistEndret()
        val enhet = if (avklaringsbehov != null) {
            val oppgaveEnhetListe = oppgavestyringGateway.hentOppgaveEnhet(behandling.referanse).oppgaver
            enhetForDefinisjon(avklaringsbehov.definisjon, oppgaveEnhetListe)
        } else {
            null
        }
        return SignaturGrunnlag(navIdent = innloggetBruker.ident, rolle = null, enhet = enhet)
    }

    private fun utledSignaturV2(
        rolle: Rolle,
        avklaringsbehovene: Avklaringsbehovene,
        oppgaveEnhetListe: List<OppgaveEnhetDto>,
        innloggetBruker: Bruker
    ): UtledetSignatur? {
        return signaturFraLøstAvklaringsbehov(avklaringsbehovene, rolle, oppgaveEnhetListe)
        // hvis ingen avklaringsbehov er løst av rolle tidligere antar vi eventuelt åpent avklaringsbehov skal løses
        // av innlogget bruker
            ?: signaturFraÅpentAvklaringsbehov(avklaringsbehovene, rolle, oppgaveEnhetListe, innloggetBruker)
    }

    private data class UtledetSignatur(
        val definisjon: Definisjon,
        val navIdent: String,
        val rolle: Rolle,
        val enhet: String?,
        val harLøstAvklaringsbehov: Boolean,
    ) {
        fun tilGrunnlag(): SignaturGrunnlag {
            return SignaturGrunnlag(
                navIdent = navIdent,
                rolle = mapRolle(rolle),
                enhet = enhet,
            )
        }
    }

    private fun signaturFraLøstAvklaringsbehov(
        avklaringsbehovene: Avklaringsbehovene,
        rolle: Rolle,
        oppgaveEnhetListe: List<OppgaveEnhetDto>,
    ): UtledetSignatur? {
        return avklaringsbehovForRolle(avklaringsbehovene, rolle).mapNotNull { avklaringsbehov ->
            val endring = sisteEndringAvPerson(avklaringsbehov) ?: return@mapNotNull null
            val enhet = enhetForDefinisjon(avklaringsbehov.definisjon, oppgaveEnhetListe)
            endring.tidsstempel to UtledetSignatur(
                definisjon = avklaringsbehov.definisjon,
                navIdent = endring.endretAv,
                rolle = rolle,
                enhet = enhet,
                harLøstAvklaringsbehov = true,
            )
        }.maxByOrNull { it.first }?.second
    }

    private fun avklaringsbehovForRolle(avklaringsbehovene: Avklaringsbehovene, rolle: Rolle): List<Avklaringsbehov> {
        val definisjoner = rolleTilAvklaringsbehov.getValue(rolle)
        return avklaringsbehovene.hentBehovForDefinisjon(definisjoner)
    }

    private fun signaturFraÅpentAvklaringsbehov(
        avklaringsbehovene: Avklaringsbehovene,
        rolle: Rolle,
        oppgaveEnhetListe: List<OppgaveEnhetDto>,
        innloggetBruker: Bruker
    ): UtledetSignatur? {
        return avklaringsbehovForRolle(avklaringsbehovene, rolle).filter { it.erÅpent() }
            .sistEndret()
            ?.let { avklaringsbehov ->
                val enhet = enhetForDefinisjon(avklaringsbehov.definisjon, oppgaveEnhetListe)
                UtledetSignatur(
                    definisjon = avklaringsbehov.definisjon,
                    navIdent = innloggetBruker.ident,
                    rolle = rolle,
                    enhet = enhet,
                    harLøstAvklaringsbehov = false,
                )
            }
    }

    private fun List<Avklaringsbehov>.sistEndret(): Avklaringsbehov? {
        return this.maxByOrNull { it.sistEndret() }
    }

    private fun sisteEndringAvPerson(avklaringsbehov: Avklaringsbehov): Endring? {
        return avklaringsbehov.historikk
            .filter { it.endretAv.erNavIdent() && it.status == AvklaringsbehovStatus.AVSLUTTET }
            .maxOrNull()
    }

    private fun enhetForDefinisjon(
        definisjon: Definisjon,
        oppgaveEnhetListe: List<OppgaveEnhetDto>,
    ): String? {
        return oppgaveEnhetListe.find { it.avklaringsbehovKode == definisjon.kode.name }?.enhet
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

private fun String.erNavIdent(): Boolean {
    return this.matches(Regex("\\w\\d{6}"))
}
