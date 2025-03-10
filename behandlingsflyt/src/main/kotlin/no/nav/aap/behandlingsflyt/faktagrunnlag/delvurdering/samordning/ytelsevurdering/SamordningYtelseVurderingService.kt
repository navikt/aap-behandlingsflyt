package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse as ForeldrePengerYtelse

class SamordningYtelseVurderingService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository,
    private val sakService: SakService,
) : Informasjonskrav {
    private val fpGateway = GatewayProvider.provide<ForeldrepengerGateway>()
    private val spGateway = GatewayProvider.provide<SykepengerGateway>()
    private val log = LoggerFactory.getLogger(SamordningYtelseVurderingService::class.java)

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator
        val foreldrepenger = hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        val sykepenger = hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        val eksisterendeData = samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
        val samordningYtelser = mapTilSamordningYtelse(foreldrepenger, sykepenger)

        if (harEndringerIYtelser(eksisterendeData, samordningYtelser)) {
            samordningYtelseVurderingRepository.lagreYtelser(kontekst.behandlingId, samordningYtelser)
            return Informasjonskrav.Endret.ENDRET
        }

        return Informasjonskrav.Endret.IKKE_ENDRET
    }

    private fun hentYtelseForeldrepenger(
        personIdent: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<ForeldrePengerYtelse> {
        return fpGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(
                Aktør(personIdent),
                Periode(fom, tom)
            )
        ).ytelser
    }

    private fun hentYtelseSykepenger(personIdent: String, fom: LocalDate, tom: LocalDate): List<UtbetaltePerioder> {
        return spGateway.hentYtelseSykepenger(
            SykepengerRequest(
                setOf(personIdent),
                fom,
                tom
            )
        ).utbetaltePerioder
    }

    private fun harEndringerIYtelser(
        eksisterende: SamordningYtelseVurderingGrunnlag?,
        samordningYtelser: List<SamordningYtelse>
    ): Boolean {
        return samordningYtelser != eksisterende?.ytelseGrunnlag?.ytelser
    }

    private fun mapTilSamordningYtelse(
        foreldrepenger: List<ForeldrePengerYtelse>,
        sykepenger: List<UtbetaltePerioder>
    ): List<SamordningYtelse> {
        val foreldrepengerKildeMapped = foreldrepenger
            .filter { konverterFraForeldrePengerDomene(it) != null }
            .map { ytelse ->
                SamordningYtelse(
                    ytelseType = konverterFraForeldrePengerDomene(ytelse)!!,
                    ytelsePerioder = ytelse.anvist.map {
                        SamordningYtelsePeriode(
                            periode = it.periode,
                            gradering = Prosent(it.utbetalingsgrad.verdi.toInt()),
                            kronesum = it.beløp
                        )
                    },
                    kilde = ytelse.kildesystem,
                    saksRef = ytelse.saksnummer.toString()
                )
            }


        val sykepengerKilde = "INFOTRYGDSPEIL"
        val sykepengerYtelse = if (sykepenger.isEmpty()) {
            null
        } else {
            SamordningYtelse(ytelseType = Ytelse.SYKEPENGER, ytelsePerioder = sykepenger.map {
                SamordningYtelsePeriode(
                    Periode(it.fom, it.tom),
                    Prosent(it.grad.toInt()),
                    null
                )
            }, kilde = sykepengerKilde)
        }

        return foreldrepengerKildeMapped.plus(listOfNotNull(sykepengerYtelse))
    }

    private fun konverterFraForeldrePengerDomene(ytelse: ForeldrePengerYtelse): Ytelse? {
        return when (ytelse.ytelse) {
            Ytelser.PLEIEPENGER_SYKT_BARN -> Ytelse.PLEIEPENGER
            Ytelser.PLEIEPENGER_NÆRSTÅENDE -> Ytelse.PLEIEPENGER
            Ytelser.OMSORGSPENGER -> Ytelse.OMSORGSPENGER
            Ytelser.OPPLÆRINGSPENGER -> Ytelse.OPPLÆRINGSPENGER
            Ytelser.ENGANGSTØNAD -> null
            Ytelser.FORELDREPENGER -> Ytelse.FORELDREPENGER
            Ytelser.SVANGERSKAPSPENGER -> Ytelse.SVANGERSKAPSPENGER
        }
    }

    companion object : Informasjonskravkonstruktør {
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            return true
        }

        override fun konstruer(connection: DBConnection): SamordningYtelseVurderingService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return SamordningYtelseVurderingService(
                repositoryProvider.provide(),
                SakService(sakRepository),
            )
        }
    }
}