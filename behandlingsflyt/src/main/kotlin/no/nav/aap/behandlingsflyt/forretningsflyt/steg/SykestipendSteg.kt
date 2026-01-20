package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenlovgivning.SamordningAnnenLovgivningFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenlovgivning.SamordningAnnenLovgivningVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.søkerOppgirStudentstatus
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

class SykestipendSteg private constructor(
    private val studentRepository: StudentRepository,
    private val sykestipendRepository: SykestipendRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        studentRepository = repositoryProvider.provide(),
        sykestipendRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
        val sykestipendGrunnlag = sykestipendRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (unleashGateway.isDisabled(BehandlingsflytFeature.Sykestipend)) {
            return Fullført
        }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.AVKLAR_SAMORDNING_SYKESTIPEND,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                                && studentGrunnlag.søkerOppgirStudentstatus()
                                && studentGrunnlag?.vurderinger?.any { it.erOppfylt() } == true

                    VurderingType.REVURDERING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                                && (studentGrunnlag?.vurderinger?.any { it.erOppfylt() } == true
                                || sykestipendRepository.hentHvisEksisterer(kontekst.behandlingId) != null)
                                && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()

                    VurderingType.UTVID_VEDTAKSLENGDE,
                    VurderingType.MIGRER_RETTIGHETSPERIODE,
                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.AUTOMATISK_BREV,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                sykestipendGrunnlag != null
            },
            tilbakestillGrunnlag = {
                val vedtatteVurdering = kontekst.forrigeBehandlingId
                    ?.let { sykestipendRepository.hentHvisEksisterer(it) }
                    ?.vurdering
                if (vedtatteVurdering != null) {
                    sykestipendRepository.lagre(kontekst.behandlingId, vedtatteVurdering)
                } else {
                    sykestipendRepository.deaktiverGrunnlag(kontekst.behandlingId)
                }

            },
            kontekst
        )

        vurderSamordningAnnenLovgivningVilkår(kontekst)

        return Fullført
    }

    // Bør kanskje inn i et eget steg?
    private fun vurderSamordningAnnenLovgivningVilkår(kontekst: FlytKontekstMedPerioder) {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING)

        val grunnlag = SamordningAnnenLovgivningFaktagrunnlag(
            kontekst.rettighetsperiode,
            sykestipendRepository.hentHvisEksisterer(kontekst.behandlingId),
        )

        SamordningAnnenLovgivningVilkår(vilkårsresultat).vurder(grunnlag)
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SykestipendSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_SYKESTIPEND
        }
    }
}
