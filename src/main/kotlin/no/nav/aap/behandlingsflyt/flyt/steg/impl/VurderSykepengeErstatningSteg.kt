package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykepengerErstatningVilkår
import no.nav.aap.behandlingsflyt.sak.SakService

class VurderSykepengeErstatningSteg(
    private val behandlingService: BehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakService: SakService
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val behandling = behandlingService.hent(kontekst.behandlingId)
        val sak = sakService.hent(kontekst.sakId)

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...
        if (bistandsvilkåret.vilkårsperioder().all { !it.erOppfylt() } &&
            sykdomsvilkåret.vilkårsperioder().any { it.erOppfylt() }) {

            val grunnlag = SykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)

            if (grunnlag?.vurdering != null) {
                val vurderingsdato = sak.rettighetsperiode.fom
                val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                    vurderingsdato,
                    vurderingsdato.plusMonths(6),
                    grunnlag.vurdering
                )
                SykepengerErstatningVilkår(vilkårsresultat).vurder(faktagrunnlag)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
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
}
