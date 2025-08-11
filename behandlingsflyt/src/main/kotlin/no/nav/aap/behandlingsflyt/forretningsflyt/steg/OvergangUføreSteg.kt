package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.lookup.repository.RepositoryProvider

class OvergangUføreSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val sykdomRepository: SykdomRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val sykdomsvurderinger = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurderinger ?: emptyList()

        if (!erIkkeAvslagPåVilkårTidligere(vilkårsresultat, sykdomsvurderinger)) {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val overgangUforeBehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.OVERGANG_UFORE)
            if (overgangUforeBehov != null && overgangUforeBehov.erÅpent()) {
                avklaringsbehovene.avbryt(Definisjon.OVERGANG_UFORE)
            }
            return Fullført
        }

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                        .avbrytForSteg(type())
                    return Fullført
                }

               /* refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return FantAvklaringsbehov(
                    Definisjon.OVERGANG_UFORE
                )*/
            }

            VurderingType.REVURDERING -> {
                //refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return FantAvklaringsbehov(Definisjon.REFUSJON_KRAV)
            }

            VurderingType.MELDEKORT -> {
                // Do nothing
            }

            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun erIkkeAvslagPåVilkårTidligere(
        vilkårsresultat: Vilkårsresultat, sykdomsvurderinger: List<Sykdomsvurdering>
    ): Boolean {
        val aldersvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        val nedsattVissVarighet = sykdomsvurderinger.any { it.erNedsettelseIArbeidsevneAvEnVissVarighet == true }

        val aldersvilkåretErOppfyltEllerIkkeVissVarighet = if (!nedsattVissVarighet) {
            true
        } else {
            aldersvilkåret.harPerioderSomErOppfylt()
        }


        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
             && vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomErOppfylt()
             && sykdomsvurderinger.any { it.erOppfyltSettBortIfraVissVarighet() }
             && aldersvilkåretErOppfyltEllerIkkeVissVarighet
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return OvergangUføreSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_UFORE
        }
    }
}
