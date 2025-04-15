package no.nav.aap.behandlingsflyt.faktagrunnlag

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.Instant

class InformasjonskravGrunnlagImpl(
    private val informasjonskravRepository: InformasjonkskravRepository,
    private val connection: DBConnection
) : InformasjonskravGrunnlag {
    private val tracer = GlobalOpenTelemetry.getTracer("informasjonskrav")

    private class Foo(
        val konstruktør: Informasjonskravkonstruktør,
        val krav: Informasjonskrav,
        val steg: StegType,
    )

    override fun oppdaterFaktagrunnlagForKravliste(
        kravkonstruktører: List<Pair<StegType, Informasjonskravkonstruktør>>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør> {
        val informasjonskravene = kravkonstruktører.map { (steg, konstruktør) ->
            Foo(
                konstruktør = konstruktør,
                krav = konstruktør.konstruer(connection),
                steg = steg,
            )
        }
        val oppdateringer =
            informasjonskravRepository.hentOppdateringer(kontekst.sakId, informasjonskravene.map { it.konstruktør.navn })

        val relevanteInformasjonskrav = informasjonskravene
            .filter { foo ->
                val sisteOppdatering = oppdateringer.firstOrNull { it.navn == foo.krav.navn }
                foo.krav.erRelevant(kontekst, foo.steg, sisteOppdatering)
            }

        val endredeInformasjonskrav = relevanteInformasjonskrav
            .filter { foo ->
                val span = tracer.spanBuilder("informasjonskrav ${foo.krav.navn}")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("informasjonskrav", foo.krav.navn.toString())
                    .startSpan()
                try {
                    span.makeCurrent().use {
                        foo.krav.oppdater(kontekst) == Informasjonskrav.Endret.ENDRET
                    }
                } finally {
                    span.end()
                }
            }

        informasjonskravRepository.registrerOppdateringer(
            kontekst.sakId,
            kontekst.behandlingId,
            relevanteInformasjonskrav.map { it.krav.navn },
            Instant.now()
        )
        return endredeInformasjonskrav.map { it.konstruktør }
    }

}
