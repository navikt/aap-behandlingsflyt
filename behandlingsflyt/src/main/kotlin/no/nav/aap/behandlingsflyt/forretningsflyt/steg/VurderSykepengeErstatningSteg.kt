package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom.SykepengerErstatningVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykepengeErstatningSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val sakService: SakService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...
                if (bistandsvilkåret.vilkårsperioder().all { !it.erOppfylt() } && (
                            sykdomsvilkåret.vilkårsperioder()
                                .any { it.erOppfylt() || avslagPåVissVarighet(it) })) {

                    val grunnlag = sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)

                    if (grunnlag?.vurdering != null) {
                        val sak = sakService.hent(kontekst.sakId)
                        val vurderingsdato = sak.rettighetsperiode.fom
                        val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                            vurderingsdato,
                            sak.rettighetsperiode.tom, // TODO: Trenger å finne en god løsning for hvordan vi setter slutt på dette vilkåret ved tom kvote
                            grunnlag.vurdering()!!
                        )
                        SykepengerErstatningVilkår(vilkårsresultat).vurder(faktagrunnlag)
                        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
                    } else {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
                    }
                } else {
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                    val sykepengeerstatningsBehov =
                        avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)

                    if (sykepengeerstatningsBehov?.erÅpent() == true) {
                        avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
                    }
                }
            }

            VurderingType.REVURDERING -> {
                // TODO: Dette må gjøres mye mer robust og sjekkes konsistent mot 11-6...
                if (bistandsvilkåret.vilkårsperioder().all { !it.erOppfylt() } && (
                            sykdomsvilkåret.vilkårsperioder()
                                .any { it.erOppfylt() || avslagPåVissVarighet(it) })) {

                    val grunnlag = sykepengerErstatningRepository.hentHvisEksisterer(kontekst.behandlingId)

                    if (grunnlag?.vurdering != null) {
                        val sak = sakService.hent(kontekst.sakId)
                        val vurderingsdato = sak.rettighetsperiode.fom
                        val faktagrunnlag = SykepengerErstatningFaktagrunnlag(
                            vurderingsdato,
                            sak.rettighetsperiode.tom, // TODO: Trenger å finne en god løsning for hvordan vi setter slutt på dette vilkåret ved tom kvote
                            grunnlag.vurdering()!!
                        )
                        SykepengerErstatningVilkår(vilkårsresultat).vurder(faktagrunnlag)
                        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
                    } else {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
                    }
                } else {
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                    val sykepengeerstatningsBehov =
                        avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKEPENGEERSTATNING)

                    if (sykepengeerstatningsBehov?.erÅpent() == true) {
                        avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
                    }
                }
            }

            VurderingType.FORLENGELSE -> {
                // !!! RIP !!!
                // Her blir det en del logikk for å vite om dette vilkåret faktisk skal forlengelse
                // Er kvoten tom? osv osv
            }

            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }



        return Fullført
    }

    private fun avslagPåVissVarighet(vilkårsperiode: Vilkårsperiode): Boolean =
        vilkårsperiode.utfall == Utfall.IKKE_OPPFYLT && vilkårsperiode.avslagsårsak == Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val avklaringsbehovRepository =
                repositoryProvider.provide<AvklaringsbehovRepository>()
            val vilkårsresultatRepository =
                repositoryProvider.provide<VilkårsresultatRepository>()

            return VurderSykepengeErstatningSteg(
                vilkårsresultatRepository,
                repositoryProvider.provide(),
                SakService(sakRepository),
                avklaringsbehovRepository
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_SYKEPENGEERSTATNING
        }
    }
}
