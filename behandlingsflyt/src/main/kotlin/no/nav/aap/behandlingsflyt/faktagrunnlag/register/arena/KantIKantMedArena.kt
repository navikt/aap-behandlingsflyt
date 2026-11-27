package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.nesteHverdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantSøknadKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.kravtidslinje
import no.nav.aap.komponenter.type.Periode

/**
 * Finner kravet som er kant i kant med Arena. Overgangen fra Arena går fra dagen etter maksdato til og med
 * første hverdag etter maksdato, slik at maksdato fredag og krav fra mandag regnes som kant i kant.
 * Helligdager regnes som hverdager, som ellers i Kelvin.
 *
 * Kravtidslinjen avgrenser hvert krav ved starten på neste krav. Vi velger det tidligste segmentet som
 * overlapper overgangen, slik at et krav fra lørdag vinner over et krav fra mandag. Returnerer null når
 * ingen krav overlapper overgangen, for eksempel ved opphold etter Arena-saken.
 */
fun kravVedOvergangFraArena(
    gjeldendeSøknadKrav: Set<RelevantSøknadKrav>,
    arenaMaksdato: ArenaMaksdato?,
): RelevantKrav? {
    val maksdato = arenaMaksdato?.maksdato ?: return null
    val overgangFraArena = Periode(maksdato.plusDays(1), maksdato.nesteHverdag())
    return gjeldendeSøknadKrav.kravtidslinje()
        .segmenter()
        .filter { it.periode.overlapper(overgangFraArena) }
        .minByOrNull { it.periode.fom }
        ?.verdi
}
