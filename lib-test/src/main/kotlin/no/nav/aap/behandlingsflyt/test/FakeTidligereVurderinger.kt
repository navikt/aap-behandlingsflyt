package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf

class FakeTidligereVurderinger: TidligereVurderinger {
    override fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return false
    }

    override fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return false
    }

    override fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return false
    }

    override fun behandlingsutfall(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): Tidslinje<TidligereVurderinger.Behandlingsutfall> {
        return tidslinjeOf(kontekst.rettighetsperiode to TidligereVurderinger.Behandlingsutfall.UKJENT)
    }
}