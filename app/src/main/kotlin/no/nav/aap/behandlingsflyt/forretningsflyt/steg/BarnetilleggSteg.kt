package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import org.slf4j.LoggerFactory

class BarnetilleggSteg(
    private val barnetilleggService: BarnetilleggService,
    private val barnetilleggRepository: BarnetilleggRepository
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BarnetilleggSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val barnetillegg = barnetilleggService.beregn(kontekst.behandlingId)

        barnetilleggRepository.lagre(
            kontekst.behandlingId,
            barnetillegg.segmenter()
                .map {
                    BarnetilleggPeriode(
                        it.periode,
                        it.verdi.barnMedRettTil()
                    )
                }
        )
        log.info("Barnetillegg {}", barnetillegg)

        if (barnetillegg.segmenter().any { it.verdi.harBarnTilAvklaring() }) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_BARNETILLEGG)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BarnetilleggSteg(
                BarnetilleggService(
                    SakOgBehandlingService(connection),
                    BarnRepository(connection),
                    PersonopplysningRepository(connection),
                    VilkårsresultatRepository(connection)
                ),
                BarnetilleggRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}