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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse as ForeldrePengerYtelse


class SamordningYtelseVurderingInformasjonskrav(
    private val samordningYtelseRepository: SamordningYtelseRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val fpGateway: ForeldrepengerGateway,
    private val spGateway: SykepengerGateway,
    private val sakService: SakService
) : Informasjonskrav<SamordningInput, SamordningRegisterdata>, KanTriggeRevurdering {
    private val log = LoggerFactory.getLogger(javaClass)

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
            kontekst, steg
        ) && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
    }


    data class SamordningInput(
        val person: Person,
        val rettighetsperiode: Periode,
    ) : InformasjonskravInput


    data class SamordningRegisterdata(
        val samordningYtelser: Set<SamordningYtelse>
    ) : InformasjonskravRegisterdata

    override fun klargjør(kontekst: FlytKontekstMedPerioder): SamordningInput {
        val sak = sakService.hentSakFor(kontekst.behandlingId)
        return SamordningInput(sak.person, sak.rettighetsperiode)
    }

    override fun hentData(input: SamordningInput): SamordningRegisterdata {
        val (person, rettighetsperiode) = input
        val personIdent = person.aktivIdent().identifikator
        val oppslagsPeriode = Periode(rettighetsperiode.fom.minusWeeks(4), rettighetsperiode.tom)

        val (foreldrepenger, foreldrepengerDuration) = measureTimedValue {
            hentYtelseForeldrepenger(
                personIdent, oppslagsPeriode
            )
        }

        log.info("Hentet foreldrepenger. Tok ${foreldrepengerDuration.inWholeMilliseconds} ms")

        val (sykepenger, sykepengerDuration) = measureTimedValue {
            hentYtelseSykepenger(
                personIdent, oppslagsPeriode
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
        personIdent: String, oppslagsPeriode: Periode
    ): List<ForeldrePengerYtelse> {
        return fpGateway.hentVedtakYtelseForPerson(
            ForeldrepengerRequest(
                Aktør(personIdent), oppslagsPeriode
            )
        ).ytelser.mapNotNull { ytelse ->
            val anvistInnenforPeriode = ytelse.anvist.filter {
                oppslagsPeriode.inneholder(it.periode)
            }
            if (anvistInnenforPeriode.isNotEmpty()) {
                ytelse.copy(anvist = anvistInnenforPeriode)
            } else {
                null
            }
        }
    }

    private fun hentYtelseSykepenger(personIdent: String, oppslagsPeriode: Periode): List<UtbetaltePerioder> {
        return spGateway.hentYtelseSykepenger(
            setOf(personIdent), oppslagsPeriode.fom, oppslagsPeriode.tom
        ).filter { oppslagsPeriode.inneholder((Periode(it.fom, it.tom))) }
    }

    private fun mapTilSamordningYtelse(
        foreldrepenger: List<ForeldrePengerYtelse>, sykepenger: List<UtbetaltePerioder>
    ): Set<SamordningYtelse> {
        val foreldrepengerKildeMapped =
            foreldrepenger.filter { konverterFraForeldrePengerDomene(it) != null }.map { ytelse ->
                SamordningYtelse(
                    ytelseType = konverterFraForeldrePengerDomene(ytelse)!!, ytelsePerioder = ytelse.anvist.map {
                        SamordningYtelsePeriode(
                            periode = it.periode,
                            gradering = Prosent(it.utbetalingsgrad.verdi.toInt()),
                            kronesum = it.beløp,
                        )
                    }.toSet(), kilde = ytelse.kildesystem, saksRef = ytelse.saksnummer
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
            }.toSet(), kilde = sykepengerKilde)
        }

        return foreldrepengerKildeMapped.plus(listOfNotNull(sykepengerYtelse)).toSet()
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
        val sak = sakService.hentSakFor(behandlingId)
        val samordningYtelser = hentData(SamordningInput(sak.person, sak.rettighetsperiode)).samordningYtelser

        // Ønsker ikke trigge revurdering automatisk i dette tilfellet enn så lenge
        val gikkFraNullTilTomtGrunnlag = samordningYtelser.isEmpty() && eksisterendeData == null

        return if (!gikkFraNullTilTomtGrunnlag && harEndringerIYtelser(eksisterendeData, samordningYtelser)) listOf(
            VurderingsbehovMedPeriode(Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER)
        )
        else emptyList()
    }


    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SAMORDNING_YTELSE
        private val secureLogger = LoggerFactory.getLogger("secureLog")
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): SamordningYtelseVurderingInformasjonskrav {
            return SamordningYtelseVurderingInformasjonskrav(
                repositoryProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide(),
                gatewayProvider.provide(),
                SakService(repositoryProvider)
            )
        }

        fun harEndringerIYtelser(
            eksisterende: SamordningYtelseGrunnlag?, samordningYtelser: Set<SamordningYtelse>
        ): Boolean {
            secureLogger.info("Hentet samordningytelse eksisterende ${eksisterende?.ytelser} med nye samordningsytelser ${samordningYtelser.map { it.ytelsePerioder }}  ${samordningYtelser.map { it.ytelseType.name }}")
            secureLogger.info("Overlapp " + harFullstendigOverlapp(eksisterende, samordningYtelser))
            return eksisterende == null || !harFullstendigOverlapp(eksisterende, samordningYtelser)
           // return eksisterende == null || samordningYtelser != eksisterende.ytelser
        }

    }

}

fun harFullstendigOverlapp(
    eksisterende: SamordningYtelseGrunnlag?,
    nye: Set<SamordningYtelse>
): Boolean {
    if (eksisterende == null) return false
    if (eksisterende.ytelser.size != nye.size) return false

    return eksisterende.ytelser.all { eksisterendeYtelse ->
        nye.any { nyYtelse ->
            perioderErLike(eksisterendeYtelse.ytelsePerioder, nyYtelse.ytelsePerioder)
        }
    }
}

private fun perioderErLike(
    eksisterende: Set<SamordningYtelsePeriode>,
    nye: Set<SamordningYtelsePeriode>
): Boolean {
    if (eksisterende.size != nye.size) return false

    return eksisterende.all { eks ->
        nye.any { ny ->
            eks.periode.fom.isEqual(ny.periode.fom) &&
                    eks.periode.tom.isEqual(ny.periode.tom) &&
                    eks.gradering == ny.gradering
        }
    }
}


