package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory


class BeregnTilkjentYtelseSteg private constructor(
    private val underveisRepository: UnderveisRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val samordningRepository: SamordningRepository,
    private val samordningUføreRepository: SamordningUføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        barnetilleggRepository = repositoryProvider.provide(),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        samordningRepository = repositoryProvider.provide(),
        samordningUføreRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING && tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
            return Fullført
        }

        val beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        val underveisgrunnlag = underveisRepository.hent(kontekst.behandlingId)
        val fødselsdato =
            requireNotNull(personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)?.brukerPersonopplysning?.fødselsdato)
        val barnetilleggGrunnlag = requireNotNull(barnetilleggRepository.hentHvisEksisterer(kontekst.behandlingId))
        val samordningGrunnlag = samordningRepository.hentHvisEksisterer(kontekst.behandlingId) ?: SamordningGrunnlag(
            id = 0L,
            samordningPerioder = listOf()
        )
        val samordningUføre = samordningUføreRepository.hentHvisEksisterer(kontekst.behandlingId)

        val beregnetTilkjentYtelse = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningGrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()
        tilkjentYtelseRepository.lagre(
            behandlingId = kontekst.behandlingId,
            tilkjent = beregnetTilkjentYtelse.map { TilkjentYtelsePeriode(it.periode, it.verdi) })
        log.info("Beregnet tilkjent ytelse: $beregnetTilkjentYtelse")

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return BeregnTilkjentYtelseSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.BEREGN_TILKJENT_YTELSE
        }
    }
}


