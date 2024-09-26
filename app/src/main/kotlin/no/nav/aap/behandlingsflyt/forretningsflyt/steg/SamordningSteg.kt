package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

class SamordningSteg(
    private val samordningService: SamordningService,
    private val samordningRepository: SamordningRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository

) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(SamordningSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val samordningTidslinje = samordningService.vurder(kontekst.behandlingId)

        if (samordningTidslinje.segmenter().any { segment ->
                segment.verdi.ytelsesGraderinger.any {
                    it.ytelse.type == AvklaringsType.MANUELL
                }
            }) {
            if (!samordningService.harGjortVurdering(kontekst.behandlingId)) {
                return StegResultat(listOf(Definisjon.AVKLAR_SAMORDNING_GRADERING))
            }
        }

        // Før eller etter? Hva gir mening?
        samordningRepository.lagre(
            kontekst.behandlingId,
            samordningTidslinje.segmenter()
                .map {
                    SamordningPeriode(
                        it.periode,
                        it.verdi.gradering
                    )
                }
        )

        log.info("Samordning tidslinje $samordningTidslinje")
        return StegResultat()
    }

    override fun vedTilbakeføring(kontekst: FlytKontekst) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
            avklaringsbehovene.avbryt(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return SamordningSteg(
                SamordningService(
                    SamordningYtelseVurderingRepository(connection),
                ),
                SamordningRepository(connection),
                AvklaringsbehovRepositoryImpl(connection)
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }
}