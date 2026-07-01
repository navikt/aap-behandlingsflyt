package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.StansOpphørService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsningForKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravMedDato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.utils.toHumanReadable
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class AvklaringsbehovValidering(
    private val repositoryProvider: RepositoryProvider,
    gatewayProvider: GatewayProvider
) {
    private val kravRepository: KravRepository = repositoryProvider.provide()
    private val stansOpphørService = StansOpphørService(repositoryProvider, gatewayProvider)

    fun validerPerioder(
        avklaringsbehovene: Avklaringsbehovene,
        løsning: PeriodisertAvklaringsbehovLøsning<*>,
        kontekst: FlytKontekst,
    ) {
        if (løsning.definisjon().erFrivillig()
            && løsning.løsningerForPerioder.isEmpty()
        ) {
            return
        }

        if (løsning is PeriodisertAvklaringsbehovLøsningForKrav) {
            val vedtatteVurderinger = kontekst.forrigeBehandlingId?.let {
                løsning.hentVurderinger(
                    kontekst.forrigeBehandlingId,
                    repositoryProvider
                )
            }.orEmpty()
            val nye = løsning.tilPeriodiserteVurdering(kontekst.behandlingId) + vedtatteVurderinger
            val kravMedUgyldigLøsning =
                nårKravHarLøsning(løsning.definisjon(), nye.gjeldendeVurderinger(), kontekst)
                    .segmenter()
                    .filter { !it.verdi }
            if (kravMedUgyldigLøsning.isNotEmpty()) {
                throw UgyldigForespørselException(
                    "Mangler vurderinger for krav i periodene ${
                        kravMedUgyldigLøsning.map { it.periode }.toHumanReadable()
                    }"
                )
            }
        }
        
        val behovForDefinisjon = avklaringsbehovene.hentBehovForDefinisjon(løsning.definisjon())
        if (behovForDefinisjon != null) {
            val perioderDekketAvLøsning = løsning.løsningerForPerioder.sortedBy { it.fom }
                .somTidslinje { Periode(fom = it.fom, tom = it.tom ?: Tid.MAKS) }
                .map { true }.komprimer()

            val lagredeVurderinger = kontekst.forrigeBehandlingId?.let {
                løsning.hentLagredeLøstePerioder(it, repositoryProvider)
            }.orEmpty()

            val perioderSomSkalLøses =
                behovForDefinisjon.perioderVedtaketBehøverVurdering().orEmpty().somTidslinje { it }

            val perioderDekketAvTidligereVurderinger = lagredeVurderinger.map { true }.komprimer()

            val perioderDekket = perioderDekketAvTidligereVurderinger.kombiner(
                perioderDekketAvLøsning,
                StandardSammenslåere.prioriterHøyreSideCrossJoin()
            ).komprimer()

            val perioderSomManglerLøsning =
                perioderSomSkalLøses.leftJoin(perioderDekket) { _, periodeILøsning ->
                    periodeILøsning != null
                }.filter { !it.verdi }.perioder().toSet()

            if (perioderSomManglerLøsning.isNotEmpty()) {
                throw UgyldigForespørselException("Du mangler vurdering for ${perioderSomManglerLøsning.toHumanReadable()}")
            }
        }
    }

    fun nårKravHarLøsning(
        definisjon: Definisjon,
        gjeldendeVurderinger: Tidslinje<PeriodisertVurdering>,
        kontekst: FlytKontekst,
    ): Tidslinje<Boolean> {
        val kravtidslinje =
            kravRepository.hentHvisEksisterer(kontekst.behandlingId)?.kravtidslinjeMedDato() ?: Tidslinje.empty()

        return kravtidslinje.map { segmentPeriode, krav ->
            erKravDekketAvLøsning(segmentPeriode, definisjon, kontekst, krav, gjeldendeVurderinger)
        }
    }

    private fun erKravDekketAvLøsning(
        kravPeriode: Periode,
        definisjon: Definisjon,
        kontekst: FlytKontekst,
        krav: KravMedDato,
        gjeldendeVurderinger: Tidslinje<PeriodisertVurdering>,
    ): Boolean {
        if (krav is NyttKrav) return gjeldendeVurderinger.segmenter().any { (vurderingPeriode, _) ->
            kravPeriode.inneholder(vurderingPeriode.fom)
        }

        if (kontekst.forrigeBehandlingId == null) {
            /**
             * Gir det mening å registrere gjenopptak i førstegangsbehandlingen?
             * I så fall vet vi ikke noe om stans/opphør, og kan ikke validere 
             */
            return true
        }
        val stansEllerOpphør = stansOpphørService
            .vedtattStansOpphør(kontekst.forrigeBehandlingId)
            .lastOrNull { it.fom < krav.muligRettFra }
        return when (stansEllerOpphør?.vurdering) {
            null -> true
            is Stans -> true
            is Opphør -> !definisjon.måRevurderesEtterOpphør || gjeldendeVurderinger.segmenter()
                .any { (vurderingPeriode, _) -> kravPeriode.inneholder(vurderingPeriode.fom) }
        }
    }
}