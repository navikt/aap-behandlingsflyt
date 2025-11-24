package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdUtlederService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class InstitusjonsoppholdSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val institusjonsoppholdUtlederService: InstitusjonsoppholdUtlederService,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        institusjonsoppholdRepository = repositoryProvider.provide(),
        institusjonsoppholdUtlederService = InstitusjonsoppholdUtlederService(repositoryProvider),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårGammel(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
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



        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårGammel(
            avklaringsbehovene = avklaringsbehovene,
            behandlingRepository = behandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
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

        return Fullført
    }


    private fun perioderHelseoppholdIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val harBehovForAvklaringer = institusjonsoppholdUtlederService.utled(kontekst.behandlingId)
        return harBehovForAvklaringer.perioderTilVurdering.map { it.harUavklartHelseopphold() }.filter { it.verdi }
            .komprimer().perioder().toSet()
    }

    private fun perioderSoningOppholdIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val harBehovForAvklaringer = institusjonsoppholdUtlederService.utled(kontekst.behandlingId)
        return harBehovForAvklaringer.perioderTilVurdering.map { it.harUavklartSoningsopphold() }.filter { it.verdi }
            .komprimer().perioder().toSet()
    }

    private fun perioderMedVurderingsbehovHelse(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val harBehovForAvklaringer = institusjonsoppholdUtlederService.utled(kontekst.behandlingId)

        return Tidslinje.zip2(tidligereVurderingsutfall, harBehovForAvklaringer.perioderTilVurdering)
            .mapValue { (behandlingsutfall, denneBehandling) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> denneBehandling?.helse != null // Enten er helse vurdert, eller så skal det vurderes
                }
            }
    }


    private fun perioderMedVurderingsbehovSoning(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val tidligereVurderingsutfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val harBehovForAvklaringer = institusjonsoppholdUtlederService.utled(kontekst.behandlingId)

        return Tidslinje.zip2(tidligereVurderingsutfall, harBehovForAvklaringer.perioderTilVurdering)
            .mapValue { (behandlingsutfall, denneBehandling) ->
                when (behandlingsutfall) {
                    null -> false
                    TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG -> false
                    TidligereVurderinger.Behandlingsutfall.UKJENT -> denneBehandling?.soning != null // Enten er soning vurdert, eller så skal det vurderes
                }
            }
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return InstitusjonsoppholdSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}
