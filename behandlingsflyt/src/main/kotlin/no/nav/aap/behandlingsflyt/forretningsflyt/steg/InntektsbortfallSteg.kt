package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class InntektsbortfallSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val unleashGateway: UnleashGateway,
    private val tidligereVurderinger: TidligereVurderinger,
    private val personopplysningRepository: PersonopplysningRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.MidlertidigStopp11_4)) {
            return Fullført
        }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.INNTEKTSBORTFALL,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING -> {
                        when {
                            tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, type()) -> false
                            kontekst.vurderingsbehovRelevanteForSteg.isEmpty() -> false
                            erUnder62PåVirkningsTidspunktet(kontekst) -> false
                            else -> true
                        }
                    }

                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = { erTilstrekkeligVurdert() },
            tilbakestillGrunnlag = {},
            kontekst = kontekst
        )
        return Fullført
    }

    fun erTilstrekkeligVurdert(): Boolean {
        //TODO: Implement AAP-1406
        return false
    }

    fun erUnder62PåVirkningsTidspunktet(kontekst: FlytKontekstMedPerioder): Boolean {
        val brukerPersonopplysning =
            personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

        val erUnder62PåVirkningsTidspunktet =
            brukerPersonopplysning.fødselsdato.alderPåDato(kontekst.rettighetsperiode.fom) < 62

        return erUnder62PåVirkningsTidspunktet
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return InntektsbortfallSteg(
                avklaringsbehovRepository = repositoryProvider.provide(),
                avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
                unleashGateway = gatewayProvider.provide(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
                personopplysningRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.INNTEKTSBORTFALL
        }
    }
}