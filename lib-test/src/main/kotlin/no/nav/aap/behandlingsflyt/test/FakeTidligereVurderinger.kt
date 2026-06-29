package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf

class FakeTidligereVurderinger(private val utfall: Tidslinje<TidligereVurderinger.Behandlingsutfall>? = null) :
    TidligereVurderinger {
    var avslagEllerIngenBehandlingsgrunnlag = false
    var avslag = false
    var ingenBehandlingsgrunnlag = false

    override fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return avslagEllerIngenBehandlingsgrunnlag
    }

    override fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return avslag
    }

    override fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return ingenBehandlingsgrunnlag
    }

    override fun behandlingsutfall(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType,
        etterSteg: StegType?,
    ): Tidslinje<TidligereVurderinger.Behandlingsutfall> {
        return when (utfall) {
            null -> tidslinjeOf(kontekst.rettighetsperiode to TidligereVurderinger.PotensieltOppfylt(null))
            else -> tidslinjeOf<TidligereVurderinger.Behandlingsutfall>(
                kontekst.rettighetsperiode to TidligereVurderinger.PotensieltOppfylt(
                    null
                )
            ).kombiner(
                utfall,
                StandardSammenslåere.prioriterHøyreSideCrossJoin()
            ).komprimer()
        }
    }
}