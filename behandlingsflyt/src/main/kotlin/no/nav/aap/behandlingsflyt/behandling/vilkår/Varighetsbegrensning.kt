package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.behandling.vilkår.Varighetsvurdering.VARIGHET_OK
import no.nav.aap.behandlingsflyt.behandling.vilkår.Varighetsvurdering.VARIGHET_OVERSKREDET
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.util.*

enum class Varighetsvurdering {
    VARIGHET_OK,
    VARIGHET_OVERSKREDET,
}

/** Map over en tidslinje, beriket med info om dato-til-dato-varighet.
 *
 * Når en dato-til-dato-periode er påbegynt, så vil ytterligere
 * vurderinger innenfor den perioden tre inn i samme periode.
 *
 * Per nå ingen støtte for opphør eller at perioder kan gjentas.
 */
fun <T, R> Tidslinje<T>.mapMedDatoTilDatoVarighet(
    /** `begrensetVarighet(v) == true` hvis denne vurderingen skal begrenses av
     * en dato-til-dato-periode.
     *
     * Når `begrensetVarighet(v) == true`, så vil det starte en dato-til-dato-periode,
     * med mindre det allerede finnes en pågående dato-til-dato-periode (i så fall trer
     * denne vurderingen inn i den eksisterende perioden).
     *
     * Vil typisk være `true` for en vurdering som sier at et vilkår er oppfylt, og `false`
     * for en vurdering som sier et vilkår ikke er oppfylt.
     */
    harBegrensetVarighet: (T) -> Boolean,

    /** Regn ut slutt-datoen (til-og-med) for en gitt start-dato (fra-og-med).
     *  Start-datoen (fom) vil være første segment hvor `begrensetVarighet(v) == true`.
     * */
    varighet: (fom: LocalDate) -> LocalDate,
    body: (Varighetsvurdering, T) -> R,
): Tidslinje<R> {
    val resultat = TreeSet<Segment<R>>()
    fun leggTil(periode: Periode, varighetsvurdering: Varighetsvurdering, elem: T) {
        resultat.add(Segment(periode, body(varighetsvurdering, elem)))
    }

    var datoTilDato: Periode? = null

    for (vurdering in this) {
        if (harBegrensetVarighet(vurdering.verdi)) {
            if (datoTilDato == null) {
                datoTilDato = Periode(vurdering.periode.fom, varighet(vurdering.periode.fom))
            }

            val okPeriode = vurdering.periode.overlapp(datoTilDato)
            if (okPeriode != null) {
                leggTil(okPeriode, VARIGHET_OK, vurdering.verdi)
            }

            for (overskredetPeriode in vurdering.periode.minus(datoTilDato)) {
                leggTil(overskredetPeriode, VARIGHET_OVERSKREDET, vurdering.verdi)
            }
        } else {
            leggTil(vurdering.periode, VARIGHET_OK, vurdering.verdi)
        }
    }
    return Tidslinje(resultat)
}