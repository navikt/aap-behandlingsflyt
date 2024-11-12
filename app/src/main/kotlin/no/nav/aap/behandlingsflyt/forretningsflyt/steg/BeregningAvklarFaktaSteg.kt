package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.beregning.AvklarFaktaBeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
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
    private val avklarFaktaBeregningService: AvklarFaktaBeregningService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val behandlingId = kontekst.behandlingId

        if (avklarFaktaBeregningService.skalFastsetteGrunnlag(behandlingId)) {
            val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
            if (beregningVurdering == null) {
                return FantAvklaringsbehov(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
            }
            if (erBehovForÅAvklareYrkesskade(behandlingId, beregningVurdering)) {
                return FantAvklaringsbehov(Definisjon.FASTSETT_YRKESSKADEINNTEKT)
            }
        }
        return Fullført
    }

    private fun erBehovForÅAvklareYrkesskade(
        behandlingId: BehandlingId,
        beregningGrunnlag: BeregningGrunnlag
    ): Boolean {
        val yrkesskadeVurdering = sykdomRepository.hent(behandlingId).yrkesskadevurdering

        return yrkesskadeVurdering?.erÅrsakssammenheng == true && harIkkeFastsattBeløpForAlle(
            yrkesskadeVurdering.relevanteSaker,
            beregningGrunnlag
        )
    }

    private fun harIkkeFastsattBeløpForAlle(
        relevanteSaker: List<String>,
        beregningGrunnlag: BeregningGrunnlag
    ): Boolean {
        val vurderteSaker = beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger ?: emptyList()
        return !relevanteSaker.all { sak -> vurderteSaker.any { it.referanse == sak } }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BeregningAvklarFaktaSteg(
                BeregningVurderingRepository(connection),
                SykdomRepository(connection),
                AvklarFaktaBeregningService(VilkårsresultatRepository(connection))
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
