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
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse as ForeldrePengerResponseYtelse

class SamordningYtelseVurderingService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository,
    private val sakService: SakService,
) : Informasjonskrav {
    private val fpGateway = GatewayProvider.provide<ForeldrepengerGateway>()
    private val spGateway = GatewayProvider.provide<SykepengerGateway>()

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator
        val foreldrepenger = hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        val sykepenger = hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)

        val eksisterendeData = samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
        val samordningYtelser = mapTilSamordningYtelse(foreldrepenger, sykepenger, sak.saksnummer.toString())

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
    ): List<ForeldrePengerResponseYtelse> {
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
        foreldrepenger: List<ForeldrePengerResponseYtelse>,
        sykepenger: List<UtbetaltePerioder>,
        saksNummer: String
    ): List<SamordningYtelse> {
        val samordningYtelser = mutableListOf<SamordningYtelse>()
        val sykepengerKilde = "INFOTRYGDSPEIL"

        for (ytelse in foreldrepenger) {
            val ytelsePerioder = ytelse.anvist.map {
                SamordningYtelsePeriode(
                    periode = it.periode,
                    gradering = Prosent(it.utbetalingsgrad.verdi.toInt()),
                    kronesum = it.beløp
                )
            }
            val c = konverterFraForeldrePengerDomene(ytelse)
            if (c != null) {
                samordningYtelser.add(
                    SamordningYtelse(
                        ytelseType = c,
                        ytelsePerioder = ytelsePerioder,
                        kilde = ytelse.kildesystem,
                        saksRef = ytelse.saksnummer.toString()
                    )
                )
            }
        }

        val ytelsePerioder = sykepenger.map {
            SamordningYtelsePeriode(
                Periode(it.fom, it.tom),
                Prosent(it.grad.toInt()),
                null
            )
        }

        // Sykepenger har ikke saksref, benytter samme som i vår for noe tracing
        samordningYtelser.add(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = ytelsePerioder,
                kilde = sykepengerKilde,
                saksRef = saksNummer
            )
        )

        return samordningYtelser
    }

    private fun konverterFraForeldrePengerDomene(ytelse: ForeldrePengerResponseYtelse): Ytelse? {
        // TODO
        return when (ytelse.ytelse) {
            Ytelser.PLEIEPENGER_SYKT_BARN -> Ytelse.PLEIEPENGER_BARN
            Ytelser.PLEIEPENGER_NÆRSTÅENDE -> Ytelse.PLEIEPENGER_NÆR_FAMILIE
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