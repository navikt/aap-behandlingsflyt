package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class SykdomsurderingBrevSteg internal constructor(
    private val sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type())) {
                    return Fullført
                }

                return vurder(kontekst)
            }

            VurderingType.REVURDERING -> {
                return vurder(kontekst)
            }

            VurderingType.MELDEKORT,
            VurderingType.AKTIVITETSPLIKT,
            VurderingType.IKKE_RELEVANT -> {
                return Fullført
            }
        }
    }

    fun vurder(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vurdering = sykdomsvurderingForBrevRepository.hent(kontekst.behandlingId)
        if (vurdering != null) {
            logger.info("Fant sykdomsvurdering for brev for behandling ${kontekst.behandlingId}")
            return Fullført
        }

        logger.info("Mangler sykdomsvurdering for brev for behandling ${kontekst.behandlingId}")
        return FantAvklaringsbehov(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SykdomsurderingBrevSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.SYKDOMSVURDERING_BREV
        }
    }
}