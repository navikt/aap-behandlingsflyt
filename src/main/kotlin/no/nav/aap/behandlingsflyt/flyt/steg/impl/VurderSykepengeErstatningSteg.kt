package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningTjeneste
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykepengerErstatningVilkår

class VurderSykepengeErstatningSteg(private val behandlingTjeneste: BehandlingTjeneste) : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)
        val sak = Sakslager.hent(input.kontekst.sakId)

        val sykdomsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...
        if (bistandsvilkåret.vilkårsperioder().all { !it.erOppfylt() } &&
            sykdomsvilkåret.vilkårsperioder().any { it.erOppfylt() }) {

            val grunnlag = SykepengerErstatningTjeneste.hentHvisEksisterer(behandling.id)

            if (grunnlag?.vurdering != null) {
                val vurderingsdato = sak.rettighetsperiode.fom
                val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                    vurderingsdato,
                    vurderingsdato.plusMonths(6),
                    grunnlag.vurdering
                )
                SykepengerErstatningVilkår(behandling.vilkårsresultat()).vurder(faktagrunnlag)
            } else {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKEPENGEERSTATNING))
            }
        } else {
            val avklaringsbehovene = behandling.avklaringsbehovene()
            val sykepengeerstatningsBehov =
                avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            if (sykepengeerstatningsBehov?.erÅpent() == true) {
                sykepengeerstatningsBehov.avbryt()
            }
        }

        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.VURDER_SYKEPENGEERSTATNING
    }
}
