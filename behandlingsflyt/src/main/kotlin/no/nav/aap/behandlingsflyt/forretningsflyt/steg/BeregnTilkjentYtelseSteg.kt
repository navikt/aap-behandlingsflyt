package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9Repository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Beløp
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
    private val samordningArbeidsgiverRepository: SamordningArbeidsgiverRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val reduksjon11_9Repository: Reduksjon11_9Repository,
    private val aktivitetsplikt11_9repository: Aktivitetsplikt11_9Repository,
    private val unleashGateway: UnleashGateway,

    ) : BehandlingSteg {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        barnetilleggRepository = repositoryProvider.provide(),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        samordningRepository = repositoryProvider.provide(),
        samordningUføreRepository = repositoryProvider.provide(),
        samordningArbeidsgiverRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        reduksjon11_9Repository = repositoryProvider.provide(),
        aktivitetsplikt11_9repository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
            return Fullført
        }

        val beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        val underveisgrunnlag = underveisRepository.hent(kontekst.behandlingId)
        val fødselsdato =
            requireNotNull(personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(kontekst.behandlingId)?.fødselsdato) { "Finner ikke fødselsdato. BehandlingId: ${kontekst.behandlingId}" }
        val barnetilleggGrunnlag =
            barnetilleggRepository.hentHvisEksisterer(kontekst.behandlingId) ?: BarnetilleggGrunnlag(
                perioder = emptyList()
            )
        val samordningGrunnlag = samordningRepository.hentHvisEksisterer(kontekst.behandlingId) ?: SamordningGrunnlag(
            samordningPerioder = emptySet()
        )
        val samordningUføre = samordningUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
        val samordningArbeidsgiver = samordningArbeidsgiverRepository.hentHvisEksisterer(kontekst.behandlingId)

        val grunnlag = TilkjentYtelseGrunnlag(
            fødselsdato,
            beregningsgrunnlag?.grunnlaget(),
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningGrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            unleashGateway.isEnabled(BehandlingsflytFeature.UnntakMeldepliktDesember)
        )
        val beregnetTilkjentYtelse = BeregnTilkjentYtelseService(grunnlag).beregnTilkjentYtelse()

        val aktivitetsplikt11_9Grunnlag = aktivitetsplikt11_9repository.hentHvisEksisterer(kontekst.behandlingId)
        val reduksjoner11_9 = (aktivitetsplikt11_9Grunnlag?.gjeldendeVurderinger() ?: emptyList())
            .map { vurdering -> tilReduksjon11_9(vurdering, beregnetTilkjentYtelse) }

        tilkjentYtelseRepository.lagre(
            behandlingId = kontekst.behandlingId,
            tilkjent = beregnetTilkjentYtelse.segmenter().map { TilkjentYtelsePeriode(it.periode, it.verdi) },
            faktagrunnlag = grunnlag,
            versjon = ApplikasjonsVersjon.versjon,
        )
        log.info("Beregnet tilkjent ytelse: $beregnetTilkjentYtelse")

        log.info("Lagrer ned reduksjoner pga aktivitetsplikt 11-9: [${reduksjoner11_9.size}]")
        reduksjon11_9Repository.lagre(
            behandlingId = kontekst.behandlingId,
            reduksjoner = reduksjoner11_9
        )
        return Fullført
    }

    /**
     * Dersom en vurdering er endret fra et brudd uten rimelig grunn til å ha rimelig grunn må disse tas
     * vare på og oversendes til aap-utbetal for å kunne "nulle ut" riktig. Brudd må følgelig ikke slettes etter
     * å ha blitt vedtatt.
     */
    private fun tilReduksjon11_9(
        vurdering: Aktivitetsplikt11_9Vurdering,
        beregnetTilkjentYtelse: Tidslinje<Tilkjent>
    ): Reduksjon11_9 {
        return if (vurdering.grunn == Grunn.RIMELIG_GRUNN) {
            Reduksjon11_9(dato = vurdering.dato, dagsats = Beløp(0))
        } else {
            val dagsatsSomSkalTrekkes =
                beregnetTilkjentYtelse.segment(vurdering.dato)?.verdi?.dagsatsFor11_9Reduksjon()
                    ?: error("Fant ikke tilkjent ytelse for 11-9-trekk på aktivitetsplikt-dato ${vurdering.dato}")
            Reduksjon11_9(dato = vurdering.dato, dagsats = dagsatsSomSkalTrekkes)
        }
    }


    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return BeregnTilkjentYtelseSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.BEREGN_TILKJENT_YTELSE
        }
    }
}


