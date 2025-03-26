package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
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
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // Logikkplan
        // 1.  hent vurderinger som har vært gjort tidligere
        // 2.  finn perioder av ytelser som krever manuell vurdering som ikke har blitt vurdert
        // 2.1 hvis ikke-tom -> avklaringsbehov for å vurdere manuelt
        // 2.2 for foreldrepenger: ha infokrav om oppstartdato, lag manuelt frivillig behov
        // 3.  hvis har all tilgjengelig data:
        // 3.1 lag tidslinje av prosentgradering og lagre i SamordningRepository

        val vurderinger = samordningService.hentVurderinger(behandlingId = kontekst.behandlingId)
        val ytelser = samordningService.hentYtelser(behandlingId = kontekst.behandlingId)
        val tidligereVurderinger = samordningService.tidligereVurderinger(vurderinger)

        val perioderSomIkkeHarBlittVurdert = samordningService.perioderSomIkkeHarBlittVurdert(
            ytelser, tidligereVurderinger
        )

        if (perioderSomIkkeHarBlittVurdert.isNotEmpty()) {
            log.info("Fant perioder som ikke har blitt vurdert: $perioderSomIkkeHarBlittVurdert")
            return FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        }

        val samordningTidslinje =
            samordningService.vurder(ytelser, tidligereVurderinger)

        samordningRepository.lagre(
            kontekst.behandlingId,
            samordningTidslinje.segmenter()
                .map {
                    SamordningPeriode(
                        it.periode,
                        it.verdi.gradering
                    )
                },
            SamordningYtelseVurderingGrunnlag(ytelser, vurderinger)
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
            return SamordningSteg(
                samordningService = SamordningService(
                    repositoryProvider.provide(),
                    repositoryProvider.provide()
                ),
                samordningRepository = samordningRepository,
                avklaringsbehovRepository = avklaringsbehovRepository,
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }
}