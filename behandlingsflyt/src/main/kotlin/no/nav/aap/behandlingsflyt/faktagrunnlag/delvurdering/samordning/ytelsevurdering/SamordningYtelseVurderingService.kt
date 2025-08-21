package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse as ForeldrePengerYtelse

class SamordningYtelseVurderingService(
    private val samordningYtelseRepository: SamordningYtelseRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val fpGateway: ForeldrepengerGateway,
    private val spGateway: SykepengerGateway,
    private val sakOgBehandlingService: SakOgBehandlingService,
) : Informasjonskrav, KanTriggeRevurdering {
    private val log = LoggerFactory.getLogger(javaClass)

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val eksisterendeData = samordningYtelseRepository.hentHvisEksisterer(kontekst.behandlingId)
        val samordningYtelser = hentRegisterdata(kontekst.behandlingId)

        if (harEndringerIYtelser(eksisterendeData, samordningYtelser)) {
            log.info("Oppdaterer samordning ytelser for behandling ${kontekst.behandlingId}. Ytelser funnet: ${samordningYtelser.map { it.ytelseType }}")
            samordningYtelseRepository.lagre(kontekst.behandlingId, samordningYtelser)
            return Informasjonskrav.Endret.ENDRET
        }

        return Informasjonskrav.Endret.IKKE_ENDRET
    }

    private fun hentRegisterdata(behandlingId: BehandlingId): List<SamordningYtelse> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        val personIdent = sak.person.aktivIdent().identifikator
        val foreldrepenger =
            hentYtelseForeldrepenger(personIdent, sak.rettighetsperiode.fom.minusWeeks(4), sak.rettighetsperiode.tom)
        val sykepenger =
            hentYtelseSykepenger(personIdent, sak.rettighetsperiode.fom.minusWeeks(4), sak.rettighetsperiode.tom)

        log.info("Hentet sykepenger for person i sak ${sak.saksnummer}. Antall: ${sykepenger.size}")

        return mapTilSamordningYtelse(foreldrepenger, sykepenger)
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
            setOf(personIdent),
            fom,
            tom
        )
    }

    private fun harEndringerIYtelser(
        eksisterende: SamordningYtelseGrunnlag?,
        samordningYtelser: List<SamordningYtelse>
    ): Boolean {
        return eksisterende == null || samordningYtelser != eksisterende.ytelser
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
                            kronesum = it.beløp,
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

    override fun behovForRevurdering(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val eksisterendeData = samordningYtelseRepository.hentHvisEksisterer(behandlingId)
        val samordningYtelser = hentRegisterdata(behandlingId)

        return if (harEndringerIYtelser(eksisterendeData, samordningYtelser))
            listOf(VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING))
        else
            listOf()
    }


    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SAMORDNING_YTELSE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): SamordningYtelseVurderingService {
            return SamordningYtelseVurderingService(
                repositoryProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                SakOgBehandlingService(repositoryProvider, gatewayProvider)
            )
        }
    }
}