package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Diagnose
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.MigrerVurderingFraArena
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigreringService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class VurderSykdomSteg(
    private val sykdomRepository: SykdomRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val arenaMigreringService: ArenaMigreringService,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg, AvklaringsbehovMetadataUtleder, MigrerVurderingFraArena {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sykdomRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider, gatewayProvider),
        arenaMigreringService = ArenaMigreringService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.erMigreringFraArena() && unleashGateway.isEnabled(BehandlingsflytFeature.MigererSykdomFraArenaAutomatisk)) {
            migrerVurderingFraArena(kontekst)
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov(kontekst),
            nårVurderingErRelevant = ::nårVurderingErRelevant,
            nårVurderingErGyldig = { tilstrekkeligVurdert(kontekst) },
            kontekst = kontekst,
            tilbakestillGrunnlag = {
                val vedtatteSykdomsvurderinger = kontekst.forrigeBehandlingId
                    ?.let { sykdomRepository.hentHvisEksisterer(it) }
                    ?.sykdomsvurderinger
                    ?: emptyList()
                sykdomRepository.lagre(kontekst.behandlingId, vedtatteSykdomsvurderinger)
            },
            gjeldendeVurderinger = { hentGjeldendeVurderinger(kontekst) }
        )
        return Fullført
    }

    private fun hentGjeldendeVurderinger(kontekst: FlytKontekstMedPerioder): Tidslinje<PeriodisertVurdering> {
        return sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje()
            ?.mapValue { it as PeriodisertVurdering }.orEmpty()
    }

    private fun tvingerAvklaringsbehov(kontekst: FlytKontekstMedPerioder): Set<Vurderingsbehov> {
        val forrigeOvergangArbeidGrunnlag = kontekst.forrigeBehandlingId?.let {
            overgangArbeidRepository.hentHvisEksisterer(it)
        }

        val vedtatteSykdomsvurderinger = kontekst.forrigeBehandlingId?.let {
            sykdomRepository.hentHvisEksisterer(it)?.sykdomsvurderinger ?: emptyList()
        }

        val skalTriggesVedRevurderingOvergangArbeid = forrigeOvergangArbeidGrunnlag?.vurderinger.isNullOrEmpty()
        val skalTriggesVedRevurderingStudent =
            vedtatteSykdomsvurderinger?.none { it.potensieltOppfyltStudent() } == true

        val irrelevanteVurderingsbehov = buildSet {
            if (!skalTriggesVedRevurderingStudent) add(Vurderingsbehov.REVURDER_STUDENT)
            if (!skalTriggesVedRevurderingOvergangArbeid) add(Vurderingsbehov.OVERGANG_ARBEID)
        }
        return kontekst.vurderingsbehovRelevanteForSteg - irrelevanteVurderingsbehov

    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(
            kontekst,
            type()
        )

        return tidligereVurderingsutfall.mapValue { behandlingsutfall ->
            when (behandlingsutfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                is TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    behandlingsutfall.rettighetstype == null
                }
            }
        }
    }

    override fun migrerVurderingFraArena(kontekst: FlytKontekstMedPerioder) {
        require(kontekst.erMigreringFraArena() && !Miljø.erProd()) {
            "Kan ikke migrere sykdomsvurdering fra Arena for sak ${kontekst.sakId} fordi det ikke er migrering"
        }

        val sykdomsvurderingFraArena =
            arenaMigreringService.hentSykdomsvurdering(kontekst.sakId)

        /**
         * Kun ordinær AAP støttes for migreringsgruppe 1.
         */
        require(sykdomsvurderingFraArena.ordinærAAP) {
            "Kan ikke migrere sykdomsvurdering fra Arena for sak ${kontekst.sakId} fordi den ikke er ordinær AAP"
        }

        /**
         * Skal vurderes som ordinær AAP
         */
        val vurdering = Sykdomsvurdering(
            begrunnelse = sykdomsvurderingFraArena.begrunnelse,
            vurderingenGjelderFra = kontekst.rettighetsperiode.fom,
            vurderingenGjelderTil = null,
            diagnose = Diagnose(
                kodeverk = sykdomsvurderingFraArena.diagnose.kodeverk,
                // TODO avklar hva som er riktig for hoveddiagnose
                hoveddiagnose = sykdomsvurderingFraArena.diagnose.hoveddiagnose.first(),
            ),
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            yrkesskadeBegrunnelse = null,
            vurdertAv = SYSTEMBRUKER,
            vurdertIBehandling = kontekst.behandlingId,
            opprettet = Instant.now(),
        )

        sykdomRepository.lagre(kontekst.behandlingId, listOf(vurdering))
    }

    private fun tilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)

        return sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty()
            .mapValue { it.vurderingenGjelderFra <= kontekst.rettighetsperiode.tom }
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderSykdomSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
