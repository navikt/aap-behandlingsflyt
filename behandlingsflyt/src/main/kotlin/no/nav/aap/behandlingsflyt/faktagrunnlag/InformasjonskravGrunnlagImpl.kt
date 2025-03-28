package no.nav.aap.behandlingsflyt.faktagrunnlag

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection

class InformasjonskravGrunnlagImpl(private val connection: DBConnection) : InformasjonskravGrunnlag {
    private val tracer = GlobalOpenTelemetry.getTracer("informasjonskrav")

    override fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        return kravliste
            .filter { kravtype -> kravtype.erRelevant(kontekst) }
            .filter { kravtype ->
                val span = tracer.spanBuilder("informasjonskrav ${kravtype::class.simpleName}")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("informasjonskrav", kravtype::class.simpleName ?: "null")
                    .startSpan()
                try {
                    span.makeCurrent().use {
                        kravtype.konstruer(connection).oppdater(kontekst) == Informasjonskrav.Endret.ENDRET
                    }
                } finally {
                    span.end()
                }
            }
    }
}
