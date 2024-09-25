package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekst
import java.time.LocalDate

class SamordningYtelseVurderingService(
    connection: DBConnection
): Informasjonskrav {
    private val fpGateway = ForeldrepengerGateway()
    private val spGateway = SykepengerGateway()
    private val sakService = SakService(connection)
    private val samordningYtelseVurderingRepository = SamordningYtelseVurderingRepository(connection)

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator
        val foreldrepenger = hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)
        val sykepenger = hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)

        val eksisterendeData = samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
        val samordningYtelser = mapTilSamordningYtelse(foreldrepenger, sykepenger, sak.saksnummer.toString())

        if (harEndingerIYtelser(eksisterendeData, samordningYtelser) ) {
            samordningYtelseVurderingRepository.lagreYtelser(kontekst.behandlingId, samordningYtelser)
            return false
        }

        return true
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

    private fun harEndingerIYtelser(eksisterende: SamordningYtelseVurderingGrunnlag?, samordningYtelser: List<SamordningYtelse>): Boolean {
        return samordningYtelser != eksisterende?.ytelser
    }

    private fun mapTilSamordningYtelse(foreldrepenger: ForeldrepengerResponse, sykepenger: SykepengerResponse, saksNummer: String): List<SamordningYtelse> {
        val samordningYtelser = mutableListOf<SamordningYtelse>()
        val sykepengerYtelse = "SYKEPENGER"
        val sykepengerKilde = "INFOTRYGDSPEIL"

        for(ytelse in foreldrepenger.ytelser) {
            val ytelsePerioder = ytelse.anvist.map {
                SamordningYtelsePeriode(
                    periode = it.periode,
                    gradering = Prosent(it.utbetalingsgrad.verdi.toInt()),
                    kronesum = it.beløp
                )
            }
            samordningYtelser.add(
                SamordningYtelse(
                    ytelseType = ytelse.ytelse,
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
                ytelseType = sykepengerYtelse,
                ytelsePerioder = ytelsePerioder,
                kilde = sykepengerKilde,
                saksRef = saksNummer
            )
        )

        return samordningYtelser
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): SamordningYtelseVurderingService {
            return SamordningYtelseVurderingService(connection)
        }
    }
}