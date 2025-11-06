package no.nav.aap.behandlingsflyt.faktagrunnlag

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.SøknadInformasjonskrav
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.utils.withMdc
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class InformasjonskravGrunnlagImpl(
    private val informasjonskravRepository: InformasjonskravRepository,
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
) : InformasjonskravGrunnlag {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        informasjonskravRepository = repositoryProvider.provide(),
        repositoryProvider = repositoryProvider,
        gatewayProvider
    )

    private val log = LoggerFactory.getLogger(javaClass)
    private val tracer = GlobalOpenTelemetry.getTracer("informasjonskrav")

    override fun oppdaterFaktagrunnlagForKravliste(
        kravkonstruktører: List<Pair<StegType, Informasjonskravkonstruktør>>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        val informasjonskravene = kravkonstruktører.map { (steg, konstruktør) ->
            Triple(
                konstruktør,
                konstruktør.konstruer(repositoryProvider, gatewayProvider),
                steg,
            )
        }
        val oppdateringer = informasjonskravRepository.hentOppdateringer(
            kontekst.sakId,
            informasjonskravene.map { (konstruktør, _, _) -> konstruktør.navn },
        )

        val relevanteInformasjonskrav = informasjonskravene
            .filter { (_, krav, steg) ->
                val sisteOppdatering = oppdateringer.firstOrNull { it.navn == krav.navn }
                krav.erRelevant(kontekst, steg, sisteOppdatering)
            }

        val executor = Executors.newVirtualThreadPerTaskExecutor()

        // TODO: Finn en bedre måde å forhindre race conditions når async informasjonskrav avghenger av hverandre
        // Når SøknadService er relevant, må denne kjøre før de andre for å forhindre race conditions
        val søknadInformasjonskrav = informasjonskravene.find { it.second.navn == SøknadInformasjonskrav.navn }
        val søknadInformasjonRelevantOgEndret =
            if (SøknadInformasjonskrav.navn in relevanteInformasjonskrav.map { it.second.navn } && søknadInformasjonskrav != null) {
                log.info("Sjekker søknadsinformasjonskrav for endringer")
                val span = tracer.spanBuilder("informasjonskrav ${SøknadInformasjonskrav.navn}")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("informasjonskrav", SøknadInformasjonskrav.navn.toString())
                    .startSpan()
                try {
                    span.makeCurrent().use {
                        val søknadInformasjonskrav =
                            SøknadInformasjonskrav.konstruer(repositoryProvider, gatewayProvider)
                        val registerdata = søknadInformasjonskrav.hentData(IngenInput)
                        val input = søknadInformasjonskrav.klargjør(kontekst)
                        søknadInformasjonskrav.oppdater(input, registerdata, kontekst) == Informasjonskrav.Endret.ENDRET
                    }
                } finally {
                    span.end()
                }
            } else {
                false
            }

        log.info("Sjekker andre informasjonskrav for endringer")

        val sekvensiellKlargjøring = relevanteInformasjonskrav
            .filter { !it.second.equals(SøknadInformasjonskrav) } // ikke kjør SøknadService dobbelt
            .map { (konstruktør, krav) ->
                Triple(konstruktør, krav, krav.klargjør(kontekst))
            }

        val parallellFaktaInnhenting = sekvensiellKlargjøring
            .filter { (_, krav) -> !krav.equals(SøknadInformasjonskrav) } // ikke kjør SøknadService dobbelt
            .map { (konstruktør, informasjonskrav, input) ->
                CompletableFuture.supplyAsync(withMdc {
                    val span = tracer.spanBuilder("informasjonskravinnhenting ${informasjonskrav.navn}")
                        .setSpanKind(SpanKind.INTERNAL)
                        .setAttribute("informasjonskrav", informasjonskrav.navn.toString())
                        .startSpan()
                    try {
                        span.makeCurrent().use {
                            val registerdata = informasjonskrav.hentData(input)
                            Triple(konstruktør, informasjonskrav, registerdata)
                        }
                    } finally {
                        span.end()
                    }
                }, executor)
            }

        val sekvensiellLagringAvFakta = parallellFaktaInnhenting
            .map { it.join() }
            .map { (konstruktør, krav, registerdata) ->
                val input = krav.klargjør(kontekst)
                Pair(konstruktør, krav.oppdater(input, registerdata, kontekst))
            }


        val endredeAsyncInformasjonskrav = sekvensiellLagringAvFakta
            .filter { (_, endret) -> endret == Informasjonskrav.Endret.ENDRET }
            .map { it.first }

        val endredeInformasjonskrav =
            if (søknadInformasjonRelevantOgEndret && søknadInformasjonskrav != null) {
                endredeAsyncInformasjonskrav + søknadInformasjonskrav.first
            } else {
                endredeAsyncInformasjonskrav
            }

        log.info("Registrerer oppdateringer fra informasjonskrav")
        informasjonskravRepository.registrerOppdateringer(
            kontekst.sakId,
            kontekst.behandlingId,
            relevanteInformasjonskrav.map { (_, krav, _) -> krav.navn },
            Instant.now(),
            kontekst.rettighetsperiode,
        )
        return endredeInformasjonskrav
    }

    override fun flettOpplysningerFraAtomærBehandling(
        kontekst: FlytKontekst,
        informasjonskravkonstruktørere: List<Informasjonskravkonstruktør>
    ): List<Informasjonskravkonstruktør> {
        return informasjonskravkonstruktørere.filter {
            it.konstruer(repositoryProvider, gatewayProvider)
                .flettOpplysningerFraAtomærBehandling(kontekst) == Informasjonskrav.Endret.ENDRET
        }
    }
}