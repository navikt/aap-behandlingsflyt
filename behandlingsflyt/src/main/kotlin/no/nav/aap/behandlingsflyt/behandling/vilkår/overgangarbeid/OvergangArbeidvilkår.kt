package no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode


class OvergangArbeidvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangArbeidFaktagrunnlag> {
    val sykdomsvurderingTidslinje = grunnlag.sykdomsvurderinger
        .sortedBy { it.opprettet }
        .map { vurdering ->
            Tidslinje(
                Periode(
                    fom = vurdering.vurderingenGjelderFra ?: grunnlag.kravDato,
                    tom = grunnlag.sisteDagMedMuligYtelse
                ),
                vurdering
            )
        }
        .fold(Tidslinje<Sykdomsvurdering>()) { t1, t2 ->
            t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }

    override fun vurder(grunnlag: OvergangArbeidFaktagrunnlag) {
        val segment = regelTidslinje.segment(grunnlag.vurderingsdato)
        if (segment == null) {
            throw IllegalArgumentException("Fant ikke regler for vurderingsdato ${grunnlag.vurderingsdato}")
        }
        val regel = segment.verdi

        regel.vurder(grunnlag)
    }

    private fun harOvergangArbeidVurdertTilGodkjent(
        overgangArbeidVurdering: OvergangArbeidVurdering?,
    ): Boolean {
        if (overgangArbeidVurdering == null) {
            return false
        }
        return overgangArbeidVurdering.run {
            virkningsDato != null &&
                    brukerRettPaaAAP == true
        }
    }
}
