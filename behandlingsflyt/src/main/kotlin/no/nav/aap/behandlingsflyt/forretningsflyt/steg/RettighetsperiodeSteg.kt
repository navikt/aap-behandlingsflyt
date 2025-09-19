package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.oppdaterAvklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø.erProd
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class RettighetsperiodeSteg constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakService: SakService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val rettighetsperiodeRepository: VurderRettighetsperiodeRepository,
    private val erProd: Boolean = erProd()
) : BehandlingSteg {

    private val logger = LoggerFactory.getLogger(RettighetsperiodeSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        logger.info("Utfører rettighetsperiodesteg for behandling=${kontekst.behandlingId}")

        if (erProd) {
            return gammelUtfør(kontekst)
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val rettighetsperiodeVurdering = rettighetsperiodeRepository.hentVurdering(kontekst.behandlingId)

        oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.VURDER_RETTIGHETSPERIODE,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                                && manueltTriggetVurderingsbehov(kontekst)

                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                rettighetsperiodeVurdering != null
            },
            tilbakestillGrunnlag = {
                val vedtattVurdering = kontekst.forrigeBehandlingId
                    ?.let { rettighetsperiodeRepository.hentVurdering(it) }
                rettighetsperiodeRepository.lagreVurdering(kontekst.behandlingId, vedtattVurdering)
            },
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                if (tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) && manueltTriggetVurderingsbehov(kontekst)) {
                    oppdaterVilkårsresultatForNyPeriode(kontekst)
                }
            }

            VurderingType.IKKE_RELEVANT,
            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> {
                // Ikke relevant
            }
        }
        return Fullført
    }

    private fun gammelUtfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovene.avbrytForSteg(type())
                    return Fullført
                }

                if (manueltTriggetVurderingsbehov(kontekst)) {
                    if (erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)) {
                        avklaringsbehovene.avbrytÅpneAvklaringsbehov()
                        return FantAvklaringsbehov(Definisjon.VURDER_RETTIGHETSPERIODE)
                    } else {
                        oppdaterVilkårsresultatForNyPeriode(kontekst)
                    }
                }
            }

            VurderingType.IKKE_RELEVANT, VurderingType.MELDEKORT, VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> {
                // Ikke relevant
            }
        }

        return Fullført
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.VURDER_RETTIGHETSPERIODE)
    }

    private fun manueltTriggetVurderingsbehov(kontekst: FlytKontekstMedPerioder): Boolean {
        if (kontekst.vurderingsbehovRelevanteForSteg.contains(Vurderingsbehov.VURDER_RETTIGHETSPERIODE)) {
            return true
        }

        // HELHETLIG_VURDERING skal kun trigge avklaringsbehov dersom det tidligere er lagt inn overstyring av
        // rettighetsperiode. Hvis ikke må alle behandlinger vurdere denne ved helhetlig vurdering.
        if (kontekst.vurderingsbehovRelevanteForSteg.contains(Vurderingsbehov.HELHETLIG_VURDERING)
            && rettighetsperiodeRepository.hentVurdering(kontekst.behandlingId) != null) {
            return true
        }
        return false
    }

    private fun oppdaterVilkårsresultatForNyPeriode(kontekst: FlytKontekstMedPerioder) {
        // TODO: Hva hvis man innskrenker perioden - må innskrenke vilkår og underveis?
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val rettighetsperiode = sakService.hent(kontekst.sakId).rettighetsperiode

        Vilkårtype
            .entries
            .filter { it.obligatorisk }
            .forEach { vilkårstype ->
                vilkårsresultat
                    .leggTilHvisIkkeEksisterer(vilkårstype)
                    .leggTilIkkeVurdertPeriode(rettighetsperiode)
                    .fjernHvisUtenforRettighetsperiode(rettighetsperiode)

            }

        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return RettighetsperiodeSteg(
                vilkårsresultatRepository = repositoryProvider.provide(),
                sakService = SakService(repositoryProvider),
                avklaringsbehovRepository = repositoryProvider.provide(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
                rettighetsperiodeRepository = repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_RETTIGHETSPERIODE
        }

        override fun toString(): String {
            return "FlytSteg(type:${type()})"
        }

    }
}
