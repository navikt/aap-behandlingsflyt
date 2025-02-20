package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.beregning.AvklarFaktaBeregningService
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class FastsettGrunnlagSteg(
    private val beregningService: BeregningService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklarFaktaBeregningService: AvklarFaktaBeregningService,
    private val sakRepository: SakRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(FastsettGrunnlagSteg::class.java)


    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.GRUNNLAGET)
        val rettighetsperiode = sakRepository.hent(kontekst.sakId).rettighetsperiode

        if (kontekst.skalBehandlesSomEntenFørstegangsbehandlingEllerRevurdering()) {
            if (avklarFaktaBeregningService.skalFastsetteGrunnlag(kontekst.behandlingId)) {
                val beregningsgrunnlag = beregningService.beregnGrunnlag(kontekst.behandlingId)

                vilkår.leggTilVurdering(
                    Vilkårsperiode(
                        periode = rettighetsperiode,
                        utfall = Utfall.OPPFYLT, // TODO: Ta med utfall av beregning hvis bruker er over 62 elns
                        manuellVurdering = false,
                        begrunnelse = null,
                        innvilgelsesårsak = null, // TODO: Sett hjemmel
                        avslagsårsak = null,
                        faktagrunnlag = beregningsgrunnlag.faktagrunnlag()
                    )
                )
                log.info("Beregnet grunnlag til ${beregningsgrunnlag.grunnlaget()}")
            } else {
                log.info("Deaktiverer grunnlag når det ikke er relevant å beregne")
                beregningService.deaktiverGrunnlag(kontekst.behandlingId)

                vilkår.leggTilVurdering(
                    Vilkårsperiode(
                        periode = rettighetsperiode,
                        utfall = Utfall.IKKE_RELEVANT,
                        manuellVurdering = false,
                        begrunnelse = null,
                        innvilgelsesårsak = null,
                        avslagsårsak = null,
                        faktagrunnlag = null
                    )
                )
            }
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return FastsettGrunnlagSteg(
                BeregningService(
                    repositoryProvider.provide(),
                    repositoryProvider.provide<SykdomRepository>(),
                    repositoryProvider.provide(),
                    repositoryProvider.provide(),
                    BeregningsgrunnlagRepositoryImpl(connection),
                    beregningVurderingRepository,
                    repositoryProvider.provide()
                ),
                vilkårsresultatRepository,
                AvklarFaktaBeregningService(vilkårsresultatRepository),
                sakRepository
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_GRUNNLAG
        }
    }
}