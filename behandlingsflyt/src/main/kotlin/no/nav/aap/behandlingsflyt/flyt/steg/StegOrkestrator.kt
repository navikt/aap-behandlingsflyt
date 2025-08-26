package no.nav.aap.behandlingsflyt.flyt.steg

import io.micrometer.core.instrument.Timer
import io.opentelemetry.api.GlobalOpenTelemetry
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.function.Supplier

/**
 * Håndterer den definerte prosessen i et gitt steg, flytter behandlingen gjennom de forskjellige fasene internt i et
 * steg. Et steg beveger seg gjennom flere faser som har forskjellig ansvar.
 *
 * @see StegStatus.START:            Teknisk markør for at flyten har flyttet seg til et gitt steg
 *
 * @see StegStatus.UTFØRER:          Utfører forrettningslogikken i steget ved å kalle på
 * @see no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg.utfør
 *
 * @see StegStatus.AVKLARINGSPUNKT:  Vurderer om maskinen har bedt om besluttningstøtte fra
 * et menneske og stopper prosessen hvis det er et punkt som krever stopp i dette steget.
 *
 * @see StegStatus.AVSLUTTER:        Teknisk markør for avslutting av steget
 */
class StegOrkestrator(
    private val informasjonskravGrunnlag: InformasjonskravGrunnlag,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val stegKonstruktør: StegKonstruktør,
    markSavepointAt: Set<StegStatus>? = null,
) {
    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        markSavepointAt: Set<StegStatus>? = null,
    ) : this(
        informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(repositoryProvider, gatewayProvider),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        stegKonstruktør = StegKonstruktørImpl(repositoryProvider, gatewayProvider),
        markSavepointAt = markSavepointAt,
    )

    private val markSavepointAt = markSavepointAt ?: setOf(StegStatus.START, StegStatus.OPPDATER_FAKTAGRUNNLAG)

    private val log = LoggerFactory.getLogger(javaClass)
    private val tracer = GlobalOpenTelemetry.getTracer("stegorkestrator")

    fun utfør(
        aktivtSteg: FlytSteg,
        kontekstMedPerioder: FlytKontekstMedPerioder,
        behandling: Behandling,
        faktagrunnlagForGjeldendeSteg: List<Pair<StegType, Informasjonskravkonstruktør>>,
    ): Transisjon {
        val stegSpan = tracer.spanBuilder("utfør ${aktivtSteg.type().name}")
            .setAttribute("steg", aktivtSteg.type().name)
            .startSpan()
        try {
            MDC.putCloseable("stegType", aktivtSteg.type().name).use {
                var gjeldendeStegStatus = StegStatus.START
                log.info(
                    "Behandler steg '{}'. Behandling-ref: ${behandling.referanse}. Vurderingtype: ${kontekstMedPerioder.vurderingType}",
                    aktivtSteg.type()
                )


                while (true) {
                    val statusSpan = tracer.spanBuilder("steg status ${gjeldendeStegStatus.name}")
                        .startSpan()
                    try {
                        MDC.putCloseable("stegStatus", gjeldendeStegStatus.name).use {
                            val resultat = utførTilstandsEndring(
                                aktivtSteg,
                                kontekstMedPerioder,
                                gjeldendeStegStatus,
                                behandling,
                                faktagrunnlagForGjeldendeSteg
                            )
                            if (gjeldendeStegStatus in markSavepointAt) {
                                // Legger denne her slik at vi får savepoint på at vi har byttet steg, slik at vi starter opp igjen på rett sted når prosessen dras i gang igjen
                                behandlingRepository.markerSavepoint()
                            }

                            if (gjeldendeStegStatus == StegStatus.AVSLUTTER) {
                                return resultat
                            }

                            if (!resultat.kanFortsette() || resultat.erTilbakeføring()) {
                                return resultat
                            }
                            gjeldendeStegStatus = gjeldendeStegStatus.neste()
                        }
                    } finally {
                        statusSpan.end()
                    }
                }
            }
        } finally {
            stegSpan.end()
        }
    }

    fun utførTilbakefør(
        aktivtSteg: FlytSteg,
        kontekstMedPerioder: FlytKontekstMedPerioder,
        behandling: Behandling
    ): Transisjon {
        val stegSpan = tracer.spanBuilder("utførTilbakefør ${aktivtSteg.type().name}")
            .setAttribute("steg", aktivtSteg.type().name)
            .startSpan()

        try {
            MDC.putCloseable("stegType", aktivtSteg.type().name).use {
                MDC.putCloseable("stegStatus", StegStatus.TILBAKEFØRT.name).use {
                    return utførTilstandsEndring(
                        aktivtSteg,
                        kontekstMedPerioder,
                        StegStatus.TILBAKEFØRT,
                        behandling,
                        emptyList()
                    )
                }
            }
        } finally {
            stegSpan.end()
        }
    }

    private fun utførTilstandsEndring(
        aktivtSteg: FlytSteg,
        kontekst: FlytKontekstMedPerioder,
        gjeldendeStegStatus: StegStatus,
        behandling: Behandling,
        faktagrunnlagForGjeldendeSteg: List<Pair<StegType, Informasjonskravkonstruktør>>
    ): Transisjon {
        val behandlingSteg = stegKonstruktør.konstruer(aktivtSteg)

        log.debug(
            "Behandler steg({}) med status({})",
            aktivtSteg.type(),
            gjeldendeStegStatus
        )

        val transisjon = when (gjeldendeStegStatus) {
            StegStatus.START -> Fortsett
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, behandlingSteg, kontekst)
            StegStatus.OPPDATER_FAKTAGRUNNLAG -> oppdaterFaktagrunnlag(kontekst, faktagrunnlagForGjeldendeSteg)
            StegStatus.AVKLARINGSPUNKT -> harAvklaringspunkt(aktivtSteg, kontekst.behandlingId)
            StegStatus.AVSLUTTER -> Fortsett
            StegStatus.TILBAKEFØRT -> behandleStegBakover(behandlingSteg, kontekst)
        }

        val nyStegTilstand = StegTilstand(stegType = aktivtSteg.type(), stegStatus = gjeldendeStegStatus, aktiv = true)
        oppdaterStegOgStatus(behandling, nyStegTilstand)

        return transisjon
    }

    private fun oppdaterFaktagrunnlag(
        kontekstMedPerioder: FlytKontekstMedPerioder,
        faktagrunnlagForGjeldendeSteg: List<Pair<StegType, Informasjonskravkonstruktør>>,
    ): Fortsett {
        informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
            faktagrunnlagForGjeldendeSteg,
            kontekstMedPerioder
        )
        return Fortsett
    }

    private fun behandleSteg(
        aktivtSteg: FlytSteg,
        behandlingSteg: BehandlingSteg,
        kontekstMedPerioder: FlytKontekstMedPerioder
    ): Transisjon {
        val simpleName = behandlingSteg.javaClass.simpleName
        val utførStegTimer =
            Timer.builder("behandlingsflyt_utfør_steg_tid")
                .tags("steg", simpleName)
                .publishPercentileHistogram()
                .register(prometheus)
        val stegResultat =
            utførStegTimer.record(Supplier {
                behandlingSteg.utfør(kontekstMedPerioder)
            })
        log.info("Fullført steg av type $simpleName med resultat $stegResultat")

        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val resultat = stegResultat.transisjon()

        if (resultat is FunnetAvklaringsbehov) {
            log.info(
                "Fant avklaringsbehov: {}",
                resultat.avklaringsbehov()
            )
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekstMedPerioder.behandlingId)
            avklaringsbehovene.leggTil(resultat.avklaringsbehov(), aktivtSteg.type())
        } else if (resultat is FunnetVentebehov) {
            log.info(
                "Fant ventebehov: {}",
                resultat.ventebehov()
            )
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekstMedPerioder.behandlingId)
            resultat.ventebehov().forEach {
                avklaringsbehovene.leggTil(
                    definisjoner = listOf(it.definisjon),
                    funnetISteg = aktivtSteg.type(),
                    frist = it.frist,
                    grunn = it.grunn
                )
            }
        }

        return resultat
    }

    private fun harAvklaringspunkt(
        aktivtSteg: FlytSteg,
        behandlingId: BehandlingId
    ): Transisjon {
        val relevanteAvklaringsbehov =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId).alle()
                .filter { it.erÅpent() }
                .filter { behov -> behov.skalLøsesISteg(aktivtSteg.type()) }


        if (relevanteAvklaringsbehov.any { behov -> behov.skalStoppeHer(aktivtSteg.type()) }) {
            return Stopp
        }

        return Fortsett
    }

    private fun behandleStegBakover(behandlingSteg: BehandlingSteg, kontekst: FlytKontekstMedPerioder): Transisjon {
        behandlingSteg.vedTilbakeføring(kontekst)
        return Fortsett
    }

    private fun oppdaterStegOgStatus(
        behandling: Behandling,
        nyStegTilstand: StegTilstand
    ) {
        behandling.oppdaterSteg(nyStegTilstand)
        behandlingRepository.leggTilNyttAktivtSteg(behandlingId = behandling.id, nyStegTilstand)
    }
}
