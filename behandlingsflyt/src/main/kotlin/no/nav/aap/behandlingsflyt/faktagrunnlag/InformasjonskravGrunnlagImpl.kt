package no.nav.aap.behandlingsflyt.faktagrunnlag

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class InformasjonskravGrunnlagImpl(
    private val informasjonskravRepository: InformasjonkskravRepository,
    private val repositoryProvider: RepositoryProvider,
) : InformasjonskravGrunnlag {
    constructor(repositoryProvider: RepositoryProvider): this(
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

        val endredeInformasjonskrav = relevanteInformasjonskrav
            .filter { (_, krav, _) ->
                val span = tracer.spanBuilder("informasjonskrav ${krav.navn}")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("informasjonskrav", krav.navn.toString())
                    .startSpan()
                try {
                    span.makeCurrent().use {
                        krav.oppdater(kontekst) == Informasjonskrav.Endret.ENDRET
                    }
                } finally {
                    span.end()
                }
            }

        informasjonskravRepository.registrerOppdateringer(
            kontekst.sakId,
            kontekst.behandlingId,
            relevanteInformasjonskrav.map { (_, krav, _) -> krav.navn },
            Instant.now()
        )
        return endredeInformasjonskrav.map { (konstruktør, _, _) -> konstruktør }
    }

}
