package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.samordning.AvklaringsType
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class SamordningSteg(
    private val samordningService: SamordningService,
    private val samordningRepository: SamordningRepository,
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository

) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(SamordningSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // Logikkplan
        // 1.  hent vurderinger som har vært gjort tidligere
        // 2.  finn perioder av ytelser som krever manuell vurdering som ikke har blitt vurdert
        // 2.1 hvis ikke-tom -> avklaringsbehov for å vurdere manuelt
        // 2.2 for foreldrepenger: ha infokrav om oppstartdato, lag manuelt frivillig behov
        // 3.  hvis har all tilgjengelig data:
        // 3.1 lag tidslinje av prosentgradering og lagre i SamordningRepository

        val faktaGrunnlag =
            samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return Fullført

        val hentedeYtelserByManuelleYtelser =
            faktaGrunnlag.ytelser.filter { it.ytelseType.type == AvklaringsType.MANUELL }.map { ytelse ->
                Tidslinje(ytelse.ytelsePerioder.map { Segment(it.periode, Pair(ytelse.ytelseType, it)) })
            }.fold(Tidslinje.empty<List<Pair<Ytelse, SamordningYtelsePeriode>>>()) { acc, curr ->
                acc.kombiner(curr, slåSammenTilListe())
            }

        val vurderinger =
            faktaGrunnlag.vurderinger.filter { it.ytelseType.type == AvklaringsType.MANUELL }.map { ytelse ->
                Tidslinje(ytelse.vurderingPerioder.map { Segment(it.periode, Pair(ytelse.ytelseType, it)) })
            }.fold(Tidslinje.empty<List<Pair<Ytelse, SamordningVurderingPeriode>>>()) { acc, curr ->
                acc.kombiner(curr, slåSammenTilListe())
            }

        val perioderSomIkkeHarBlittVurdert =
            hentedeYtelserByManuelleYtelser.kombiner(vurderinger, StandardSammenslåere.minus())

        if (perioderSomIkkeHarBlittVurdert.isNotEmpty()) {
            log.info("Fant perioder som ikke har blitt vurdert: $perioderSomIkkeHarBlittVurdert")
            return FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING)
        }

        // Nå ingen flere avklaringer å gjøre, så kan regne ut gradering

        val hentedeYtelserFraRegister =
            faktaGrunnlag.ytelser.map { ytelse ->
                Tidslinje(ytelse.ytelsePerioder.map { Segment(it.periode, Pair(ytelse.ytelseType, it)) })
            }.fold(Tidslinje.empty<List<Pair<Ytelse, SamordningYtelsePeriode>>>()) { acc, curr ->
                acc.kombiner(curr, slåSammenTilListe())
            }

        // Slå sammen med vurderinger og regn ut graderinger

        val samordningTidslinje =
            hentedeYtelserFraRegister.kombiner(vurderinger, JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
                // Vi har allerede verifisert at periodene overlapper
                requireNotNull(venstre)
                requireNotNull(høyre)

                val manueltVurderteGraderinger =
                    høyre.verdi.associate { it.first to it.second }.mapValues { it.value.gradering!! }
                        .filterKeys { it.type == AvklaringsType.MANUELL }

                val registerVurderinger = venstre.verdi.associate { it.first to it.second.gradering!! }
                    .filterKeys { it.type == AvklaringsType.AUTOMATISK }

                val gradering =
                    manueltVurderteGraderinger.plus(registerVurderinger).values.sumOf { it.prosentverdi() }
                Segment(periode, Prosent(gradering))
            })

        if (!samordningTidslinje.isEmpty()) {
            samordningRepository.lagre(
                kontekst.behandlingId,
                samordningTidslinje.segmenter()
                    .map {
                        SamordningPeriode(
                            it.periode,
                            it.verdi
                        )
                    }
            )
        }


        log.info("Samordning tidslinje $samordningTidslinje")
        return Fullført
    }

    private fun <E> slåSammenTilListe(): JoinStyle.OUTER_JOIN<List<Pair<Ytelse, E>>, Pair<Ytelse, E>, List<Pair<Ytelse, E>>> =
        JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (venstre == null && høyre == null) {
                null
            } else if (venstre != null && høyre == null) {
                Segment(periode, venstre.verdi)
            } else if (høyre != null && venstre == null) {
                Segment(periode, listOf(høyre.verdi))
            } else {
                Segment(periode, venstre?.verdi.orEmpty() + listOfNotNull(høyre?.verdi))
            }
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
                SamordningService(repositoryProvider.provide()),
                samordningRepository,
                repositoryProvider.provide(),
                avklaringsbehovRepository
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }
}