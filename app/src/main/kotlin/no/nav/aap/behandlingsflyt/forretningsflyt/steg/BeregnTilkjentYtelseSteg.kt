package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import org.slf4j.LoggerFactory


class BeregnTilkjentYtelseSteg private constructor(
    private val underveisRepository: UnderveisRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BeregnTilkjentYtelseSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId)
        val underveisgrunnlag = underveisRepository.hent(kontekst.behandlingId)
        val fødselsdato = requireNotNull(personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)?.brukerPersonopplysning?.fødselsdato)
        val barnetilleggGrunnlag = requireNotNull(barnetilleggRepository.hentHvisEksisterer(kontekst.behandlingId))

        val beregnetTilkjentYtelse = BeregnTilkjentYtelseService(fødselsdato, beregningsgrunnlag, underveisgrunnlag, barnetilleggGrunnlag).beregnTilkjentYtelse()
        tilkjentYtelseRepository.lagre(behandlingId = kontekst.behandlingId, tilkjent = beregnetTilkjentYtelse)
        log.info("Beregnet tilkjent ytelse: $beregnetTilkjentYtelse")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BeregnTilkjentYtelseSteg(
                UnderveisRepository(connection),
                BeregningsgrunnlagRepository(connection),
                PersonopplysningRepository(connection),
                BarnetilleggRepository(connection),
                TilkjentYtelseRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.BEREGN_TILKJENT_YTELSE
        }
    }
}


