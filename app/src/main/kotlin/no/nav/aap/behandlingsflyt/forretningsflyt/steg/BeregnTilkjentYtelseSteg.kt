package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory


class BeregnTilkjentYtelseSteg private constructor(
    private val underveisRepository: UnderveisRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val personopplysningRepository: PersonopplysningRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BeregnTilkjentYtelseSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val beregningsgrunnlag = requireNotNull(beregningsgrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId))
        val underveisgrunnlag = underveisRepository.hent(kontekst.behandlingId)
        val fødselsdato = requireNotNull(personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)?.personopplysning?.fødselsdato)

        val beregnetTilkjentYtelse = BeregnTilkjentYtelseService(fødselsdato, beregningsgrunnlag, underveisgrunnlag).beregnTilkjentYtelse()

        log.info("Beregnet tilkjent ytelse: $beregnetTilkjentYtelse")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BeregnTilkjentYtelseSteg(
                UnderveisRepository(connection),
                BeregningsgrunnlagRepository(connection),
                PersonopplysningRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.BEREGN_TILKJENT_YTELSE
        }
    }
}

class Tilkjent(val dagsats: Beløp, val gradering: Prosent) {

    fun redusertDagsats(): Beløp {
        return dagsats.multiplisert(gradering)
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tilkjent

        if (dagsats != other.dagsats) return false
        if (gradering != other.gradering) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dagsats.hashCode()
        result = 31 * result + gradering.hashCode()
        return result
    }
}

class TilkjentGUnit(val dagsats: GUnit, val gradering: Prosent) {

    fun redusertDagsats(): GUnit {
        return dagsats.multiplisert(gradering)
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()})"
    }
}
