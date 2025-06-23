package no.nav.aap.behandlingsflyt.faktagrunnlag

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class InformasjonskravGrunnlagImpl(
    private val informasjonskravRepository: InformasjonkskravRepository,
    private val repositoryProvider: RepositoryProvider,
) : InformasjonskravGrunnlag {
    constructor(repositoryProvider: RepositoryProvider) : this(
        informasjonskravRepository = repositoryProvider.provide(),
        repositoryProvider = repositoryProvider,
    )

    private val tracer = GlobalOpenTelemetry.getTracer("informasjonskrav")

    override fun oppdaterFaktagrunnlagForKravliste(
        kravkonstruktører: List<Pair<StegType, Informasjonskravkonstruktør>>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        val informasjonskravene = kravkonstruktører.map { (steg, konstruktør) ->
            Triple(
                konstruktør,
                konstruktør.konstruer(repositoryProvider),
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

        val informasjonskravFutures = relevanteInformasjonskrav
            .map { triple ->
                CompletableFuture.supplyAsync({
                    val krav = triple.second
                    val span = tracer.spanBuilder("informasjonskrav ${krav.navn}")
                        .setSpanKind(SpanKind.INTERNAL)
                        .setAttribute("informasjonskrav", krav.navn.toString())
                        .startSpan()
                    try {
                        span.makeCurrent().use {
                            Pair(triple, krav.oppdater(kontekst))
                        }
                    } finally {
                        span.end()
                    }
                }, executor)
            }

        val endredeInformasjonskrav = informasjonskravFutures.map { it.join() }
            .filter { (_, endret) -> endret == Informasjonskrav.Endret.ENDRET }
            .map { it.first }

        informasjonskravRepository.registrerOppdateringer(
            kontekst.sakId,
            kontekst.behandlingId,
            relevanteInformasjonskrav.map { (_, krav, _) -> krav.navn },
            Instant.now()
        )
        return endredeInformasjonskrav.map { (konstruktør, _, _) -> konstruktør }
    }

    override fun flettOpplysningerFraAtomærBehandling(
        kontekst: FlytKontekst,
        informasjonskravkonstruktørere: List<Informasjonskravkonstruktør>
    ): List<Informasjonskravkonstruktør> {
        return informasjonskravkonstruktørere.filter {
            it.konstruer(repositoryProvider)
                .flettOpplysningerFraAtomærBehandling(kontekst) == Informasjonskrav.Endret.ENDRET
        }
    }
}