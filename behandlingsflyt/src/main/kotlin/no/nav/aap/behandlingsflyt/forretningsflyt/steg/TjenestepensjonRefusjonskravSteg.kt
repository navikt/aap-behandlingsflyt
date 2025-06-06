package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
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

class TjenestepensjonRefusjonskravSteg private constructor(
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tjenestePensjonRepository: TjenestePensjonRepository,
    private val tidligereVurderinger: TidligereVurderinger,
): BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        tjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tjenestePensjonRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider)
    )


    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                        .avbrytForSteg(type())
                    return Fullført
                }
                val tpResultat = tjenestePensjonRepository.hent(kontekst.behandlingId)
                val tidligereTpVurderinger = tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

                if (tidligereTpVurderinger == null && tpResultat.isNotEmpty()) return FantAvklaringsbehov(
                    Definisjon.SAMORDNING_REFUSJONS_KRAV
                )
            }
            VurderingType.REVURDERING -> {
                //Do nothing
            }

            VurderingType.MELDEKORT -> {
                //Do nothing
            }

            VurderingType.IKKE_RELEVANT -> {
                //Do nothing
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return TjenestepensjonRefusjonskravSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_TJENESTEPENSJON_REFUSJONSKRAV
        }
    }
}