package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

class SamordningSteg(
    private val samordningService: SamordningService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository

) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(SamordningSteg::class.java)

    private val erUvurdert = false //TODO: Finn ut når er uvurdert
    // TODO: Finnes det noe som gjør at vi ikke trenger å vurdere manuell ytelsesgradering?
    private val kanLukkes = false //TODO: Fiks

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val samordningTidslinje = samordningService.vurder(kontekst.behandlingId)

        // Hvis perioden har ytelsesgradering som er manuell,
        // så skal det opprettes et avklaringsbehov hvis ikke allerede vurdert
        if (samordningTidslinje.segmenter().any {
                it.verdi.ytelsesGraderinger.any {
                    it.ytelse.type == AvklaringsType.MANUELL
                }
            }) {
            // Sjekk om det finnes perioder som ikke er vurdert
            if (erUvurdert) {
                return StegResultat(listOf(Definisjon.AVKLAR_SAMORDNING_GRADERING))
            } else {
                // Sjekk om det finnes avklaringsbehov
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SAMORDNING_GRADERING)
                // Sjekk om de kan lukkes
                if (avklaringsbehov != null && avklaringsbehov.erÅpent() && kanLukkes) {
                    avklaringsbehovene.avbryt(Definisjon.AVKLAR_SAMORDNING_GRADERING)
                }
            }
        }
        log.info("Samordning tidslinje $samordningTidslinje")
        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return SamordningSteg(
                SamordningService(
                    SamordningRepository(connection),
                    SamordningYtelseVurderingRepository(connection),
                ),
                AvklaringsbehovRepositoryImpl(connection)
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }
}