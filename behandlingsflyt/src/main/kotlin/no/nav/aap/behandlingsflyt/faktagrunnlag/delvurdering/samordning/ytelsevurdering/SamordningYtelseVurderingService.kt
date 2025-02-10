package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class SamordningYtelseVurderingService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository,
    private val sakService: SakService
) : Informasjonskrav {
    private val fpGateway = ForeldrepengerGateway()
    private val spGateway = SykepengerGateway()

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator
        val foreldrepenger = hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        val sykepenger = hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)

        val eksisterendeData = samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
        val samordningYtelser = mapTilSamordningYtelse(foreldrepenger, sykepenger, sak.saksnummer.toString())

        if (harEndingerIYtelser(eksisterendeData, samordningYtelser)) {
            samordningYtelseVurderingRepository.lagreYtelser(kontekst.behandlingId, samordningYtelser)
            return Informasjonskrav.Endret.ENDRET
        }

        return Informasjonskrav.Endret.IKKE_ENDRET
    }

    private fun hentYtelseForeldrepenger(personIdent: String, fom: LocalDate, tom: LocalDate): ForeldrepengerResponse {
        return fpGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(
                Aktør(personIdent),
                Periode(fom, tom)
            )
        )
    }

    private fun hentYtelseSykepenger(personIdent: String, fom: LocalDate, tom: LocalDate): SykepengerResponse {
        return spGateway.hentYtelseSykepenger(
            SykepengerRequest(
                setOf(personIdent),
                fom,
                tom
            )
        )
    }

    private fun harEndingerIYtelser(
        eksisterende: SamordningYtelseVurderingGrunnlag?,
        samordningYtelser: List<SamordningYtelse>
    ): Boolean {
        return samordningYtelser != eksisterende?.ytelser
    }

    private fun mapTilSamordningYtelse(
        foreldrepenger: ForeldrepengerResponse,
        sykepenger: SykepengerResponse,
        saksNummer: String
    ): List<SamordningYtelse> {
        val samordningYtelser = mutableListOf<SamordningYtelse>()
        val sykepengerYtelse = "SYKEPENGER"
        val sykepengerKilde = "INFOTRYGDSPEIL"

        for (ytelse in foreldrepenger.ytelser) {
            val ytelsePerioder = ytelse.anvist.map {
                SamordningYtelsePeriode(
                    periode = it.periode,
                    gradering = Prosent(it.utbetalingsgrad.verdi.toInt()),
                    kronesum = it.beløp
                )
            }
            samordningYtelser.add(
                SamordningYtelse(
                    ytelseType = konverterFraForeldrePengerDomene(ytelse),
                    ytelsePerioder = ytelsePerioder,
                    kilde = ytelse.kildesystem,
                    saksRef = ytelse.saksnummer.toString()
                )
            )
        }

        val ytelsePerioder = sykepenger.utbetaltePerioder.map {
            SamordningYtelsePeriode(
                Periode(it.fom, it.tom),
                Prosent(it.grad.toInt()),
                null
            )
        }

        // Sykepenger har ikke saksref, benytter samme som i vår for noe tracing
        samordningYtelser.add(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER, // TODO, korrekt?
                ytelsePerioder = ytelsePerioder,
                kilde = sykepengerKilde,
                saksRef = saksNummer
            )
        )

        return samordningYtelser
    }

    private fun konverterFraForeldrePengerDomene(ytelse: no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse) =
        when (ytelse.ytelse) {
            Ytelser.PLEIEPENGER_SYKT_BARN -> TODO()
            Ytelser.PLEIEPENGER_NÆRSTÅENDE -> Ytelse.PLEIEPENGER_NÆR_FAMILIE
            Ytelser.OMSORGSPENGER -> Ytelse.OMSORGSPENGER
            Ytelser.OPPLÆRINGSPENGER -> Ytelse.OPPLÆRINGSPENGER
            Ytelser.ENGANGSTØNAD -> TODO()
            Ytelser.FORELDREPENGER -> Ytelse.FORELDREPENGER
            Ytelser.SVANGERSKAPSPENGER -> Ytelse.SVANGERSKAPSPENGER
            Ytelser.FRISINN -> TODO()
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
                SakService(sakRepository)
            )
        }
    }
}