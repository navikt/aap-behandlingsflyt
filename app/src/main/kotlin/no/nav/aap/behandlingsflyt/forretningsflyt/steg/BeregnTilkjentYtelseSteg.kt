package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

class BeregnTilkjentYtelseSteg(
    private val underveisRepository: UnderveisRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BeregnTilkjentYtelseSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val beregningsgrunnlag = requireNotNull(beregningsgrunnlagRepository.hentHvisEksisterer(kontekst.behandlingId))
        val underveisGrunnlag = underveisRepository.hent(kontekst.behandlingId)

        val underveisTidslinje = Tidslinje(underveisGrunnlag.perioder.map { Segment(it.periode, it) })

        val gsnittTidslinjeForUttaket =
            Grunnbeløp.tilTidslinjeGjennomsnitt().kombiner(
                underveisTidslinje,
                { periode, venstreSegment, høyreSegment ->
                    val venstre = venstreSegment?.verdi
                    if (venstre == null) {
                        null
                    } else {
                        Segment(periode, venstre)
                    }
                }, JoinStyle.INNER_JOIN
            )

        val grunnlagsfaktor = beregningsgrunnlag.grunnlaget()

        val maksDagsatsHeleUttaket =
            gsnittTidslinjeForUttaket.mapValue { verdi -> verdi?.multiplisert(grunnlagsfaktor.verdi()) }
                .kombiner(underveisTidslinje, { periode, venstreSegment, høyreSegment ->
                    val dagsats = venstreSegment?.verdi ?: Beløp(0)
                    val gradering = høyreSegment?.verdi?.gradering?.prosent ?: Prosent.`0_PROSENT`
                    Segment(periode, Tilkjent(dagsats, gradering))
                })

        log.info("Beregnet tilkjent ytelse: $maksDagsatsHeleUttaket")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BeregnTilkjentYtelseSteg(
                UnderveisRepository(connection),
                BeregningsgrunnlagRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.BEREGN_TILKJENT_YTELSE
        }
    }
}

class Tilkjent(val dagsats: Beløp, val gradering: Prosent) {

    fun tilUtbetaling(): Beløp {
        return dagsats.multiplisert(gradering.getProsent())
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, totalt=${tilUtbetaling()})"
    }


}
