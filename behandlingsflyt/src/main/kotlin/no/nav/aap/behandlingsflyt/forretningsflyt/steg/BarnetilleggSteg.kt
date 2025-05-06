package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.lookup.repository.RepositoryProvider

class BarnetilleggSteg(
    private val barnetilleggService: BarnetilleggService,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        barnetilleggService = BarnetilleggService(repositoryProvider),
        barnetilleggRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder) = when (kontekst.vurdering.vurderingType) {
        VurderingType.FØRSTEGANGSBEHANDLING -> {
            if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                    .avbrytForSteg(type())
                Fullført
            } else {
                vurder(kontekst)
            }
        }

        VurderingType.REVURDERING -> {
            vurder(kontekst)
        }

        VurderingType.MELDEKORT,
        VurderingType.IKKE_RELEVANT -> {
            /* do nothing */
            Fullført
        }
    }

    private fun vurder(kontekst: FlytKontekstMedPerioder): StegResultat {
        val barnetillegg = barnetilleggService.beregn(kontekst.behandlingId)

        barnetilleggRepository.lagre(
            kontekst.behandlingId,
            barnetillegg.segmenter()
                .map {
                    BarnetilleggPeriode(
                        it.periode,
                        it.verdi.barnMedRettTil()
                    )
                }
        )

        if (barnetillegg.segmenter().any { it.verdi.harBarnTilAvklaring() }) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_BARNETILLEGG)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return BarnetilleggSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}