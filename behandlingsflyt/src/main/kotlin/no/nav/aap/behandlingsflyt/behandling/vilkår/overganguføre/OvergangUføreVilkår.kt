package no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering


class OvergangUføreVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangUføreFaktagrunnlag> {
    val bistandsvurderinger = grunnlag.vurderinger
    override fun vurder(grunnlag: OvergangUføreFaktagrunnlag) {
        val segment = regelTidslinje.segment(grunnlag.vurderingsdato)
        if (segment == null) {
            throw IllegalArgumentException("Fant ikke regler for vurderingsdato ${grunnlag.vurderingsdato}")
        }
        val regel = segment.verdi

        regel.vurder(grunnlag)
    }

private fun harOvergangUføreVurdertTilGodkjent(
    overgangUføreVurdering: OvergangUføreVurdering?,
): Boolean {
    if (overgangUføreVurdering == null) {
        return false
    }
    return overgangUføreVurdering.run {
        virkningsDato != null && brukerSoktUforetrygd && brukerVedtakUforetrygd != "NEI" && brukerRettPaaAAP == true
    }
}}
