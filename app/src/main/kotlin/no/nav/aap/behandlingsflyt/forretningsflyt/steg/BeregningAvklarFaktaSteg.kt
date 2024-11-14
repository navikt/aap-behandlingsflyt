package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.beregning.AvklarFaktaBeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
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
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BeregningAvklarFaktaSteg private constructor(
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val sykdomRepository: SykdomRepository,
    private val vilkårsresultatRepository1: VilkårsresultatRepository,
    private val avklarFaktaBeregningService: AvklarFaktaBeregningService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId

        if (avklarFaktaBeregningService.skalFastsetteGrunnlag(behandlingId)) {
            val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
            val vilkårsresultat = vilkårsresultatRepository1.hent(behandlingId)
            if (beregningVurdering == null && erIkkeStudent(vilkårsresultat)) {
                return FantAvklaringsbehov(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
            }
            if (erBehovForÅAvklareYrkesskade(behandlingId, beregningVurdering)) {
                return FantAvklaringsbehov(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
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
        val yrkesskadeVurdering = sykdomRepository.hent(behandlingId).yrkesskadevurdering

        return yrkesskadeVurdering?.erÅrsakssammenheng == true && harIkkeFastsattBeløpForAlle(
            yrkesskadeVurdering.relevanteSaker,
            beregningGrunnlag
        )
    }

    private fun harIkkeFastsattBeløpForAlle(
        relevanteSaker: List<String>,
        beregningGrunnlag: BeregningGrunnlag?
    ): Boolean {
        val vurderteSaker = beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger ?: emptyList()
        return !relevanteSaker.all { sak -> vurderteSaker.any { it.referanse == sak } }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val vilkårsresultatRepository = VilkårsresultatRepository(connection)
            return BeregningAvklarFaktaSteg(
                BeregningVurderingRepository(connection),
                SykdomRepository(connection),
                vilkårsresultatRepository,
                AvklarFaktaBeregningService(vilkårsresultatRepository)
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
