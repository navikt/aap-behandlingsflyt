package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
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
                return FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING)
            }
        }

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
        return Fullført
    }

    override fun vedTilbakeføring(kontekst: FlytKontekstMedPerioder) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        if (avklaringsbehov != null && avklaringsbehov.erÅpent()) {
            avklaringsbehovene.avbryt(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            val samordningRepository = repositoryProvider.provide<SamordningRepository>()
            val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
            return SamordningSteg(
                SamordningService(
                    SamordningYtelseVurderingRepository(connection),
                    underveisRepository
                ),
                samordningRepository,
                avklaringsbehovRepository
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }
}