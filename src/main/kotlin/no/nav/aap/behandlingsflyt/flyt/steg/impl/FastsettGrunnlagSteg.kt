package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.beregning.BeregningService
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import org.slf4j.LoggerFactory

class FastsettGrunnlagSteg(private val beregningService: BeregningService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(FastsettGrunnlagSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val beregnGrunnlag = beregningService.beregnGrunnlag(kontekst.behandlingId)

        log.info("Beregnet grunnlag til ${beregnGrunnlag}")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FastsettGrunnlagSteg(
                BeregningService(
                    InntektGrunnlagRepository(connection),
                    SykdomRepository(connection)
                )
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_GRUNNLAG
        }
    }
}