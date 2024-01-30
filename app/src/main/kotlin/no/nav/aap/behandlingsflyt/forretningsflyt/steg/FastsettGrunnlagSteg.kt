package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.beregning.BeregningService
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.uføre.UføreRepository
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import org.slf4j.LoggerFactory

class FastsettGrunnlagSteg(
    private val beregningService: BeregningService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val periodeTilVurderingService: PeriodeTilVurderingService
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(FastsettGrunnlagSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val beregningsgrunnlag = beregningService.beregnGrunnlag(kontekst.behandlingId)

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val periodeTilVurdering = periodeTilVurderingService.utled(kontekst, Vilkårtype.GRUNNLAGET)

        val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.GRUNNLAGET)

        periodeTilVurdering.forEach { periode ->
            vilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = beregningsgrunnlag.faktagrunnlag()
                )
            )
        }

        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)

        log.info("Beregnet grunnlag til ${beregningsgrunnlag.grunnlaget()}")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FastsettGrunnlagSteg(
                BeregningService(
                    InntektGrunnlagRepository(connection),
                    SykdomRepository(connection),
                    UføreRepository(connection),
                    BeregningsgrunnlagRepository(connection)
                ),
                VilkårsresultatRepository(connection),
                PeriodeTilVurderingService(SakService(connection))
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_GRUNNLAG
        }
    }
}