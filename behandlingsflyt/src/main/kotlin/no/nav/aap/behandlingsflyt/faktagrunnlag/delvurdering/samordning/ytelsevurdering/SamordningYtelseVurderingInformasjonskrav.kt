package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav.SamordningInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav.SamordningRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Aktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.measureTimedValue
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse as ForeldrePengerYtelse

class SamordningYtelseVurderingInformasjonskrav(
    private val samordningYtelseRepository: SamordningYtelseRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val fpGateway: ForeldrepengerGateway,
    private val spGateway: SykepengerGateway,
    private val sakOgBehandlingService: SakOgBehandlingService,
) : Informasjonskrav<SamordningInput, SamordningRegisterdata>, KanTriggeRevurdering {
    private val log = LoggerFactory.getLogger(javaClass)

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
            kontekst,
            steg
        ) && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
    }


    data class SamordningInput(
        val person: Person,
        val rettighetsperiode: Periode,
    ) : InformasjonskravInput


    data class SamordningRegisterdata(
        val samordningYtelser: List<SamordningYtelse>
    ) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): SamordningInput {
        val sak = sakOgBehandlingService.hentSakFor(kontekst.behandlingId)
        return SamordningInput(sak.person, sak.rettighetsperiode)
    }

    override fun hentData(input: SamordningInput): SamordningRegisterdata {
        val (person, rettighetsperiode) = input
        val personIdent = person.aktivIdent().identifikator
        val (foreldrepenger, foreldrepengerDuration) = measureTimedValue {
            hentYtelseForeldrepenger(
                personIdent, rettighetsperiode.fom.minusWeeks(4), rettighetsperiode.tom
            )
        }

        log.info("Hentet foreldrepenger. Tok ${foreldrepengerDuration.inWholeMilliseconds} ms")

        val (sykepenger, sykepengerDuration) = measureTimedValue {
            hentYtelseSykepenger(
                personIdent, rettighetsperiode.fom.minusWeeks(4), rettighetsperiode.tom
            )
        }

        log.info("Hentet sykepenger. Tok ${sykepengerDuration.inWholeMilliseconds} ms")
        val samordningYtelser = mapTilSamordningYtelse(foreldrepenger, sykepenger)
        return SamordningRegisterdata(samordningYtelser)
    }

    override fun oppdater(
        input: SamordningInput, registerdata: SamordningRegisterdata, kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val eksisterendeData = samordningYtelseRepository.hentHvisEksisterer(kontekst.behandlingId)

        val (samordningYtelser) = registerdata

        if (harEndringerIYtelser(eksisterendeData, samordningYtelser)) {
            log.info("Oppdaterer samordning ytelser for behandling ${kontekst.behandlingId}. Ytelser funnet: ${samordningYtelser.map { it.ytelseType }}")
            samordningYtelseRepository.lagre(kontekst.behandlingId, samordningYtelser)
            return Informasjonskrav.Endret.ENDRET
        }

        return Informasjonskrav.Endret.IKKE_ENDRET
    }

    private fun hentYtelseForeldrepenger(
        personIdent: String, fom: LocalDate, tom: LocalDate
    ): List<ForeldrePengerYtelse> {
        return fpGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(
                Aktør(personIdent), Periode(fom, tom)
            )
        ).ytelser
    }

    private fun hentYtelseSykepenger(personIdent: String, fom: LocalDate, tom: LocalDate): List<UtbetaltePerioder> {
        return spGateway.hentYtelseSykepenger(
            setOf(personIdent), fom, tom
        )
    }

    private fun mapTilSamordningYtelse(
        foreldrepenger: List<ForeldrePengerYtelse>, sykepenger: List<UtbetaltePerioder>
    ): List<SamordningYtelse> {
        val foreldrepengerKildeMapped =
            foreldrepenger.filter { konverterFraForeldrePengerDomene(it) != null }.map { ytelse ->
                SamordningYtelse(
                    ytelseType = konverterFraForeldrePengerDomene(ytelse)!!, ytelsePerioder = ytelse.anvist.map {
                        SamordningYtelsePeriode(
                            periode = it.periode,
                            gradering = Prosent(it.utbetalingsgrad.verdi.toInt()),
                            kronesum = it.beløp,
                        )
                    }, kilde = ytelse.kildesystem, saksRef = ytelse.saksnummer
                )
            }


        val sykepengerKilde = "INFOTRYGDSPEIL"
        val sykepengerYtelse = if (sykepenger.isEmpty()) {
            null
        } else {
            SamordningYtelse(ytelseType = Ytelse.SYKEPENGER, ytelsePerioder = sykepenger.map {
                SamordningYtelsePeriode(
                    Periode(it.fom, it.tom), Prosent(it.grad.toInt()), null
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
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        val samordningYtelser = hentData(SamordningInput(sak.person, sak.rettighetsperiode)).samordningYtelser

        // Ønsker ikke trigge revurdering automatisk i dette tilfellet enn så lenge
        val gikkFraNullTilTomtGrunnlag = samordningYtelser.isEmpty() && eksisterendeData == null

        return if (!gikkFraNullTilTomtGrunnlag && harEndringerIYtelser(eksisterendeData, samordningYtelser)) listOf(
            VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING)
        )
        else emptyList()
    }


    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SAMORDNING_YTELSE

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): SamordningYtelseVurderingInformasjonskrav {
            return SamordningYtelseVurderingInformasjonskrav(
                repositoryProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                SakOgBehandlingService(repositoryProvider, gatewayProvider)
            )
        }

        fun harEndringerIYtelser(
            eksisterende: SamordningYtelseGrunnlag?, samordningYtelser: List<SamordningYtelse>
        ): Boolean {
            return eksisterende == null || samordningYtelser.toSet() != eksisterende.ytelser.toSet()
        }
    }
}