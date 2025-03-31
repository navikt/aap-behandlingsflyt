package no.nav.aap.behandlingsflyt.faktagrunnlag

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.Instant

class InformasjonskravGrunnlagImpl(
    private val informasjonskravRepository: InformasjonkskravRepository,
    private val connection: DBConnection
) : InformasjonskravGrunnlag {
    private val tracer = GlobalOpenTelemetry.getTracer("informasjonskrav")

    override fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        val oppdateringer = informasjonskravRepository.hentOppdateringer(kontekst.sakId, kravliste.map { it.navn })
        val relevanteInformasjonskrav = kravliste
            .filter { kravtype ->
                kravtype.erRelevant(
                    kontekst,
                    oppdateringer.firstOrNull { it.navn == kravtype.navn },
                )
            }

        val endredeInformasjonskrav = relevanteInformasjonskrav
            .filter { kravtype ->
                val informasjonskrav = kravtype.konstruer(connection)
                val span = tracer.spanBuilder("informasjonskrav ${kravtype.navn}")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("informasjonskrav", kravtype.navn.toString())
                    .startSpan()
                try {
                    span.makeCurrent().use {
                        informasjonskrav.oppdater(kontekst) == Informasjonskrav.Endret.ENDRET
                    }
                } finally {
                    span.end()
                }
            }

        informasjonskravRepository.registrerOppdateringer(kontekst.sakId, kontekst.behandlingId, relevanteInformasjonskrav.map { it.navn }, Instant.now())
        return endredeInformasjonskrav
    }

}
