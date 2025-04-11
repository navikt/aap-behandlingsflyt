package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class BarnetilleggSteg(
    private val barnetilleggService: BarnetilleggService,
    private val barnetilleggRepository: BarnetilleggRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider): this(
        barnetilleggService = BarnetilleggService(repositoryProvider),
        barnetilleggRepository = repositoryProvider.provide(),
    )
    private val log = LoggerFactory.getLogger(javaClass)

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
        log.info("Beregnet barnetillegg for behandling med id=${kontekst.behandlingId}")

        // TODO: Bør det inn sjekk om dette skjer i revurdering osv
        if (barnetillegg.segmenter().any { it.verdi.harBarnTilAvklaring() }) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_BARNETILLEGG)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BarnetilleggSteg(RepositoryProvider(connection))
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}