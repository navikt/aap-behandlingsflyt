package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.beregning.AvklarFaktaBeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class BeregningAvklarFaktaSteg private constructor(
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val sykdomRepository: SykdomRepository,
    private val vilkårsresultatRepository1: VilkårsresultatRepository,
    private val avklarFaktaBeregningService: AvklarFaktaBeregningService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val yrkesskadeRepository: YrkesskadeRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId

        if (avklarFaktaBeregningService.skalFastsetteGrunnlag(behandlingId)) {
            val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

            val vilkårsresultat = vilkårsresultatRepository1.hent(behandlingId)
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
            if (beregningVurdering == null && erIkkeStudent(vilkårsresultat)) {
                return FantAvklaringsbehov(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
            }
            val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
            if (erBehovForÅAvklareYrkesskade(behandlingId, beregningVurdering)) {
                return FantAvklaringsbehov(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
            } else if (avklaringsbehov != null) {
                avklaringsbehovene.avbryt(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
            }
        }
        return Fullført
    }

    private fun erIkkeStudent(vilkårsresultat: Vilkårsresultat): Boolean {
        return vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder()
            .firstOrNull()?.innvilgelsesårsak != Innvilgelsesårsak.STUDENT
    }

    private fun erBehovForÅAvklareYrkesskade(
        behandlingId: BehandlingId,
        beregningGrunnlag: BeregningGrunnlag?
    ): Boolean {
        val yrkesskadeVurdering = sykdomRepository.hentHvisEksisterer(behandlingId)?.yrkesskadevurdering
        val yrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        return yrkesskader?.yrkesskader?.harYrkesskade() == true && yrkesskadeVurdering?.erÅrsakssammenheng == true && harIkkeFastsattBeløpForAlle(
            yrkesskadeVurdering.relevanteSaker,
            beregningGrunnlag
        )
    }

    private fun harIkkeFastsattBeløpForAlle(
        relevanteSaker: List<String>,
        beregningGrunnlag: BeregningGrunnlag?
    ): Boolean {
        val vurderteSaker = beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger ?: emptyList()
        if (relevanteSaker.isEmpty()) {
            return false
        }
        return !relevanteSaker.all { sak -> vurderteSaker.any { it.referanse == sak } }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()
            return BeregningAvklarFaktaSteg(
                beregningVurderingRepository,
                repositoryProvider.provide(),
                vilkårsresultatRepository,
                AvklarFaktaBeregningService(vilkårsresultatRepository),
                avklaringsbehovRepository,
                YrkesskadeRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
