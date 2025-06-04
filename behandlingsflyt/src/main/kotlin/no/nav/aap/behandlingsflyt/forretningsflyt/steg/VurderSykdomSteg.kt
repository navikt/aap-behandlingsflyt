package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class VurderSykdomSteg private constructor(
    private val studentRepository: StudentRepository,
    private val sykdomRepository: SykdomRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        studentRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    log.info("Tidligere vurderinger gir ingen behandlingsgrunnlag for vilkårtype ${Vilkårtype.SYKDOMSVILKÅRET} for behandlingId ${kontekst.behandlingId}")
                    avklaringsbehovene.avbrytForSteg(type())
                    return Fullført
                }

                val vilkårResultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
                val studentVurdering = studentGrunnlag?.studentvurdering
                if (skalStoppeIFørstegangsbehandling(
                        vilkårResultat,
                        studentVurdering,
                        avklaringsbehovene
                    )
                ) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_SYKDOM)
                } else {
                    val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
                    if (avklaringsbehov != null && avklaringsbehov.erÅpent() && studentVurdering?.erOppfylt() == true) {
                        avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKDOM)
                    }
                }
            }

            VurderingType.REVURDERING -> {
                if (ÅrsakTilBehandling.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND in kontekst.årsakerTilBehandling) {
                    val forrigeBehandlingId = requireNotNull(kontekst.forrigeBehandlingId) {
                        "En revurdering skal alltid ha en en ID for forrige behandling"
                    }

                    val vurdering = sykdomRepository.hent(kontekst.behandlingId)
                    val forrigeVurdering = sykdomRepository.hent(forrigeBehandlingId)

                    if (vurdering.id() == forrigeVurdering.id()) {
                        return FantAvklaringsbehov(Definisjon.AVKLAR_SYKDOM)
                    }
                }

                if (erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_SYKDOM)
                } else {
                    val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)
                    if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
                        avklaringsbehovene.avbryt(Definisjon.AVKLAR_SYKDOM)
                    }
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.AVKLAR_SYKDOM)
    }

    private fun skalStoppeIFørstegangsbehandling(
        vilkårResultat: Vilkårsresultat,
        studentVurdering: StudentVurdering?,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        if (!erIkkeAvslagPåVilkårTidligere(vilkårResultat) || studentVurdering?.erOppfylt() == true) {
            return false
        }
        return erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)
    }

    private fun erIkkeAvslagPåVilkårTidligere(vilkårsresultat: Vilkårsresultat): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).harPerioderSomErOppfylt()
                && vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG).harPerioderSomErOppfylt()
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return VurderSykdomSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
