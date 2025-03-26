package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused")
class SamordningAvslagGrunnlag(
    val samordningGrunnlag: SamordningYtelseVurderingGrunnlag?,
    val uføreGrunnlag: UføreGrunnlag?,
) : Faktagrunnlag

class SamordningAvslagSteg(
    private val samordningService: SamordningService,
    private val uføreService: UføreService,
    private val uføreRepository: UføreRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakRepository: SakRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val sak = sakRepository.hent(kontekst.sakId)

        val samordningTidslinje = samordningService.tidslinje(kontekst.behandlingId)
        val uføre = uføreService.tidslinje(kontekst.behandlingId)

        /* NB: bevisst valg å ikke gi avslag selv om summen av samordninger blir til 100%. */
        val samordningsVurderinger = samordningTidslinje.outerJoinNotNull(uføre) { andreYtelserSamordning, uføregradering ->
            val samordningerYtelser =
                andreYtelserSamordning?.ytelsesGraderinger.orEmpty().map { it.ytelse.toString() to it.gradering }
            val samordningUføre = listOfNotNull(uføregradering?.let { "UFØRE" to it })
            val samordninger = (samordningerYtelser + samordningUføre)
                .filter { (_, prosent) -> prosent == `100_PROSENT` }

            if (samordninger.isEmpty())
                null
            else
                Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = "Full ytelse ${samordninger.joinToString { (navn, _) -> navn }}",
                    avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE,
                    faktagrunnlag = SamordningAvslagGrunnlag(
                        samordningGrunnlag = SamordningYtelseVurderingGrunnlag(
                            ytelseGrunnlag = samordningService.hentYtelser(kontekst.behandlingId),
                            vurderingGrunnlag = samordningService.hentVurderinger(kontekst.behandlingId),
                        ),
                        uføreGrunnlag = uføreRepository.hentHvisEksisterer(kontekst.behandlingId),
                    ),
                )
        }


        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SAMORDNING)
        vilkår.leggTilVurderinger(samordningsVurderinger.kryss(sak.rettighetsperiode))
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        return Fullført
    }

    override fun vedTilbakeføring(kontekst: FlytKontekstMedPerioder) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
            avklaringsbehovene.avbryt(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            return SamordningAvslagSteg(
                samordningService = SamordningService(
                    repositoryProvider.provide(),
                    repositoryProvider.provide()
                ),
                avklaringsbehovRepository = avklaringsbehovRepository,
                vilkårsresultatRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                uføreService = UføreService(
                    sakService = SakService(
                        sakRepository = repositoryProvider.provide(),
                    ),
                    uføreRepository = repositoryProvider.provide(),
                    uføreRegisterGateway = GatewayProvider.provide(),
                    sakOgBehandlingService = SakOgBehandlingService(
                        grunnlagKopierer = GrunnlagKopierer(connection),
                        sakRepository = repositoryProvider.provide(),
                        behandlingRepository = repositoryProvider.provide(),
                    ),
                ),
                uføreRepository = repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_AVSLAG
        }
    }
}