package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravSteg private constructor(
    private val refusjonkravRepository: RefusjonkravRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val sykdomRepository: SykdomRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val sykdomsvurderinger = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.sykdomsvurderinger ?: emptyList()

        if (!erIkkeAvslagPåVilkårTidligere(vilkårsresultat, sykdomsvurderinger)) {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val refusjonBehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.REFUSJON_KRAV)
            if (refusjonBehov != null && refusjonBehov.erÅpent()) {
                avklaringsbehovene.avbryt(Definisjon.REFUSJON_KRAV)
            }
            return Fullført
        }

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return FantAvklaringsbehov(Definisjon.REFUSJON_KRAV)
            }

            VurderingType.REVURDERING -> {
                //refusjonkravRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return FantAvklaringsbehov(Definisjon.REFUSJON_KRAV)
            }

            VurderingType.FORLENGELSE -> {
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
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        val nedsattVissVarighet = sykdomsvurderinger.any { it.erNedsettelseIArbeidsevneAvEnVissVarighet == true }

        val bistandsvilkåretErOppfyltEllerIkkeVissVarighet = if (!nedsattVissVarighet) {
            true
        } else {
            bistandsvilkåret.harPerioderSomErOppfylt()
        }

        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
            && vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomErOppfylt()
            && sykdomsvurderinger.any { it.erOppfyltSettBortIfraVissVarighet() }
            && bistandsvilkåretErOppfyltEllerIkkeVissVarighet
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
            return RefusjonkravSteg(refusjonkravRepository, vilkårsresultatRepository, avklaringsbehovRepository, sykdomRepository)
        }

        override fun type(): StegType {
            return StegType.REFUSJON_KRAV
        }
    }
}
