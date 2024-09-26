package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.Aktør
import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger.SykepengerRequest
import no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.flyt.FlytKontekst
import java.time.LocalDate

/*
* Informasjonselementer fra ytelsene:
* Perioder med ytelse
* utbetalingsgrad per periode
*
* -----Tables vi trenger?:------
* SamordningGrunnlag
* - bruk underveisGrunnlag table
* SamordningPerioder
* - bruk underveisPerioder table
* SamordningPeriode
* - id(pk) - gradering (smallint) - perioder_id(fk){samordningsperioder} - periode(daterange)
*
* YtelsesGradering
* - id(pk) - samordningsperiodeId(fk) - ytelse(string/enum) - gradering (smallint)
*/

class SamordningService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository
) {
    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)
        val vurderRegler = vurderRegler(samordningYtelseVurderingGrunnlag)

        return vurderRegler
    }

    fun harGjortVurdering(behandlingId: BehandlingId): Boolean {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        return samordningYtelseVurderingGrunnlag?.vurderingerId != null
            && samordningYtelseVurderingGrunnlag.vurderinger != null
            && samordningYtelseVurderingGrunnlag.vurderinger!!.isNotEmpty()
    }

    fun vurderRegler(samordning: SamordningYtelseVurderingGrunnlag?) : Tidslinje<SamordningGradering> {
        //TODO: Kombiner til tidslinje her. Kaja skal utarbeide en oversikt over regler, avventer detteø
        return Tidslinje(listOf())
    }
}