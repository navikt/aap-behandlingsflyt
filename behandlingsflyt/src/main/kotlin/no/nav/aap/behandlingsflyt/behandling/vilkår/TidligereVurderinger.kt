package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.tidslinje.Tidslinje

/** Når kan vi definitivt si at det er avslag, slik
 * at vi ikke trenger å vurdere flere vilkår.
 *
 * Det er viktig at vi kun ser fram til aktivt steg,
 * fordi selv om det er avslag på et steg senere i flyten, så kan det være
 * at den vurderingen endres slik at det ikke lenger er et avslag.
 */
interface TidligereVurderinger {
    fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun harBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return !girIngenBehandlingsgrunnlag(kontekst, førSteg)
    }

    fun muligMedRettTilAAP(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return !girAvslagEllerIngenBehandlingsgrunnlag(kontekst, førSteg)
    }

    sealed interface Behandlingsutfall
    data object IkkeBehandlingsgrunnlag : Behandlingsutfall
    data object UunngåeligAvslag : Behandlingsutfall
    data class PotensieltOppfylt(
        val rettighetstype: RettighetsType?,
        val muligRettFraNavKontor: RettighetsType? = null
    ) : Behandlingsutfall

    fun behandlingsutfall(
        kontekst: FlytKontekstMedPerioder, førSteg: StegType, etterSteg: StegType? = null
    ): Tidslinje<Behandlingsutfall>
}
