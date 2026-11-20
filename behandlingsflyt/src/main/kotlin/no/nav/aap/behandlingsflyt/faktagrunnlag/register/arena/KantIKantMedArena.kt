package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.kravtidslinje

/**
 * Finner kravet som er kant i kant med Arena: kravet hvis segment i kravtidslinjen inneholder
 * dagen etter Arena-maksdato. Kravtidslinjen avgrenser hvert krav ved starten på neste krav.
 * Returnerer null når ingen krav dekker datoen, for eksempel ved opphold etter Arena-saken.
 */
fun kravVedOvergangFraArena(
    gjeldendeRelevanteKrav: Set<RelevantKrav>,
    arenaMaksdato: ArenaMaksdato?,
): RelevantKrav? {
    val dagenEtterMaksdato = arenaMaksdato?.maksdato?.plusDays(1) ?: return null
    return gjeldendeRelevanteKrav.kravtidslinje()
        .segmenter()
        .singleOrNull { it.periode.inneholder(dagenEtterMaksdato) }
        ?.verdi
}

