package no.nav.aap.behandlingsflyt.behandling.lovvalg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeGrunnlag
import java.time.LocalDate

data class InntekterForKrav(
    // Nullable i overgangsperiode. Skal migreres til stønadsperiode.startdato
    val startdato: LocalDate?,
    val inntekter: Set<InntektINorgeGrunnlag>
) {
    /**
     * Brukes for å filtrere vekk irrelevant registerdata
     */
    fun erRelevant(stønadsperiodeGrunnlag: StønadsperiodeGrunnlag): Boolean {
        return stønadsperiodeGrunnlag.gjeldendeVurderinger()
            .filter {
                it.relevantKravType in listOf(
                    RelevantKravType.NY_STØNADSPERIODE,
                    RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR
                )
            }.any { it.startDato == startdato }
    }
}

