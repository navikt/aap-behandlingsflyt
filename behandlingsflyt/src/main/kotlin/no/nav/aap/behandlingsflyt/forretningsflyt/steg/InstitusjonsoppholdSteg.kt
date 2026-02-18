package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdUtlederService
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdUtlederServiceNy
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MapInstitusjonoppholdTilRegel
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.straffegjennomføring.StraffegjennomføringGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.straffegjennomføring.StraffegjennomføringVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class InstitusjonsoppholdSteg(
    private val institusjonsoppholdUtlederService: InstitusjonsoppholdUtlederService,
    private val institusjonsoppholdUtlederServiceNy: InstitusjonsoppholdUtlederServiceNy,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val unleashGateway: UnleashGateway
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        institusjonsoppholdRepository = repositoryProvider.provide(),
        institusjonsoppholdUtlederService = InstitusjonsoppholdUtlederService(repositoryProvider),
        institusjonsoppholdUtlederServiceNy = InstitusjonsoppholdUtlederServiceNy(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            perioderSomIkkeErTilstrekkeligVurdert = { perioderHelseoppholdIkkeErTilstrekkeligVurdert(kontekst) },
            kontekst = kontekst,
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId
                    ?.let { institusjonsoppholdRepository.hentHvisEksisterer(it) }?.helseoppholdvurderinger
                val aktiveVurderinger =
                    institusjonsoppholdRepository.hentHvisEksisterer(kontekst.behandlingId)?.helseoppholdvurderinger?.vurderinger.orEmpty()
                if (vedtatteVurderinger == null && aktiveVurderinger.isNotEmpty()) {
                    institusjonsoppholdRepository.lagreHelseVurdering(kontekst.behandlingId, "Kelvin", listOf())
                } else if (vedtatteVurderinger != null && vedtatteVurderinger.vurderinger.toSet() != aktiveVurderinger.toSet()) {
                    institusjonsoppholdRepository.lagreHelseVurdering(
                        kontekst.behandlingId,
                        vedtatteVurderinger.vurdertAv,
                        vedtatteVurderinger.vurderinger
                    )
                }
            },
            definisjon = Definisjon.AVKLAR_HELSEINSTITUSJON,
            tvingerAvklaringsbehov = setOf<Vurderingsbehov>(Vurderingsbehov.INSTITUSJONSOPPHOLD),
            nårVurderingErRelevant = ::perioderMedVurderingsbehovHelse
        )



        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            nårVurderingErRelevant = ::perioderMedVurderingsbehovSoning,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSoningOppholdIkkeErTilstrekkeligVurdert(kontekst = kontekst) },
            kontekst = kontekst,
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId
                    ?.let { institusjonsoppholdRepository.hentHvisEksisterer(it) }?.soningsVurderinger
                val aktiveVurderinger =
                    institusjonsoppholdRepository.hentHvisEksisterer(kontekst.behandlingId)?.soningsVurderinger?.vurderinger.orEmpty()
                if (vedtatteVurderinger == null && aktiveVurderinger.isNotEmpty()) {
                    institusjonsoppholdRepository.lagreSoningsVurdering(kontekst.behandlingId, "Kelvin", listOf())
                } else if (vedtatteVurderinger != null && vedtatteVurderinger.vurderinger.toSet() != aktiveVurderinger.toSet()) {
                    institusjonsoppholdRepository.lagreSoningsVurdering(
                        kontekst.behandlingId,
                        vedtatteVurderinger.vurdertAv,
                        vedtatteVurderinger.vurderinger
                    )
                }
            },
            definisjon = Definisjon.AVKLAR_SONINGSFORRHOLD,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.INSTITUSJONSOPPHOLD),
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.MIGRER_RETTIGHETSPERIODE,
            VurderingType.REVURDERING,
                -> {
                val utlederResultat = institusjonsoppholdUtlederServiceUtlederResultat(
                    kontekst.behandlingId,
                    begrensetTilRettighetsperiode = false
                )

                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                StraffegjennomføringVilkår(vilkårsresultat).vurder(
                    StraffegjennomføringGrunnlag(
                        vurderFra = kontekst.rettighetsperiode.fom,
                        institusjonsopphold = MapInstitusjonoppholdTilRegel.map(utlederResultat),
                    )
                )
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            VurderingType.MELDEKORT,
            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }

        return Fullført
    }


    private fun perioderHelseoppholdIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val harBehovForAvklaringer = institusjonsoppholdUtlederServiceUtlederResultat(kontekst.behandlingId)
        return harBehovForAvklaringer.perioderTilVurdering.map { it.harUavklartHelseopphold() }.filter { it.verdi }
            .komprimer().perioder().toSet()
    }

    private fun perioderSoningOppholdIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val harBehovForAvklaringer = institusjonsoppholdUtlederServiceUtlederResultat(kontekst.behandlingId)
        return harBehovForAvklaringer.perioderTilVurdering.map { it.harUavklartSoningsopphold() }.filter { it.verdi }
            .komprimer().perioder().toSet()
    }

    private fun perioderMedVurderingsbehovHelse(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val harBehovForAvklaringer = institusjonsoppholdUtlederServiceUtlederResultat(kontekst.behandlingId)

        return Tidslinje.zip2(tidligereVurderingsutfall, harBehovForAvklaringer.perioderTilVurdering)
            .mapValue { (behandlingsutfall, denneBehandling) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                    TidligereVurderinger.UunngåeligAvslag -> false
                    is TidligereVurderinger.PotensieltOppfylt, TidligereVurderinger.Ukjent -> denneBehandling?.helse != null // Enten er helse vurdert, eller så skal det vurderes
                }
            }
    }


    private fun perioderMedVurderingsbehovSoning(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val harBehovForAvklaringer = institusjonsoppholdUtlederServiceUtlederResultat(kontekst.behandlingId)

        return Tidslinje.zip2(tidligereVurderingsutfall, harBehovForAvklaringer.perioderTilVurdering)
            .mapValue { (behandlingsutfall, denneBehandling) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.IkkeBehandlingsgrunnlag -> false
                    TidligereVurderinger.UunngåeligAvslag -> false
                    is TidligereVurderinger.PotensieltOppfylt, TidligereVurderinger.Ukjent -> denneBehandling?.soning != null // Enten er soning vurdert, eller så skal det vurderes
                }
            }
    }

    private fun institusjonsoppholdUtlederServiceUtlederResultat(
        behandlingId: BehandlingId,
        begrensetTilRettighetsperiode: Boolean = true
    ) =
        if (unleashGateway.isEnabled(BehandlingsflytFeature.PeriodiseringHelseinstitusjonOpphold)) {
            institusjonsoppholdUtlederServiceNy.utled(
                behandlingId,
                begrensetTilRettighetsperiode = begrensetTilRettighetsperiode
            )
        } else {
            institusjonsoppholdUtlederService.utled(
                behandlingId,
                begrensetTilRettighetsperiode = begrensetTilRettighetsperiode
            )
        }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return InstitusjonsoppholdSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}
