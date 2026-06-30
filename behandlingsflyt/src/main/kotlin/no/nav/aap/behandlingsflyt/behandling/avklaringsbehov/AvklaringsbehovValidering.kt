package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.StansOpphørService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Gjenopptak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravMedDato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.utils.toHumanReadable
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
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

        val kravSomManglerLøsning = kravSomManglerLøsning(løsning, kontekst)
        if (kravSomManglerLøsning.isNotEmpty()) {
            val sisteKrav = kravSomManglerLøsning.maxBy { it.muligRettFra }
            when (sisteKrav) {
                is Gjenopptak -> throw UgyldigForespørselException("Du må sende inn en løsning fra og med eller etter ${sisteKrav.muligRettFra} i forbindelse med krav om gjennoptak")
                is NyttKrav -> throw UgyldigForespørselException("Du må sende inn en løsning fra og med eller etter ${sisteKrav.muligRettFra} i forbindelse med nytt krav")
            }
        }

        val perioderDekketAvLøsning = løsning.løsningerForPerioder.sortedBy { it.fom }
            .somTidslinje { Periode(fom = it.fom, tom = it.tom ?: Tid.MAKS) }
            .map { true }.komprimer()

        val perioderDekketAvTidligereVurderinger = kontekst.forrigeBehandlingId?.let {
            løsning.hentLagredeLøstePerioder(it, repositoryProvider)
                .map { true }.komprimer()
        }.orEmpty()

        val perioderDekket = perioderDekketAvTidligereVurderinger.kombiner(
            perioderDekketAvLøsning,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).komprimer()

        val behovForDefinisjon = avklaringsbehovene.hentBehovForDefinisjon(løsning.definisjon())
        if (behovForDefinisjon != null) {
            val perioderSomSkalLøses =
                behovForDefinisjon.perioderVedtaketBehøverVurdering().orEmpty().somTidslinje { it }

            val perioderSomManglerLøsning =
                perioderSomSkalLøses.leftJoin(perioderDekket) { _, periodeILøsning ->
                    periodeILøsning != null
                }.filter { !it.verdi }.perioder().toSet()

            if (perioderSomManglerLøsning.isNotEmpty()) {
                throw UgyldigForespørselException("Du mangler vurdering for ${perioderSomManglerLøsning.toHumanReadable()}")
            }
        }
    }

    fun kravSomManglerLøsning(
        løsning: PeriodisertAvklaringsbehovLøsning<*>,
        kontekst: FlytKontekst,
    ): List<KravMedDato> {
        val kravGrunnlag = kravRepository.hentHvisEksisterer(kontekst.behandlingId) ?: return emptyList()

        val nyeKravIDenneBehandlingen = kravGrunnlag
            .gjeldendeVurderinger()
            .filter { it.vurdertIBehandling == kontekst.behandlingId }
            .filterIsInstance<KravMedDato>()

        if (nyeKravIDenneBehandlingen.isEmpty()) return emptyList()

        return nyeKravIDenneBehandlingen.filterNot { erKravDekketAvLøsning(kontekst, it, løsning) }

    }

    private fun erKravDekketAvLøsning(
        kontekst: FlytKontekst,
        krav: KravMedDato,
        løsning: PeriodisertAvklaringsbehovLøsning<*>,
    ): Boolean {
        if (krav is NyttKrav) return validerDatoMotKrav(krav, løsning)

        if (kontekst.forrigeBehandlingId == null) {
            return true
        }
        val stansEllerOpphør = stansOpphørService.vedtattStansOpphør(kontekst.forrigeBehandlingId).lastOrNull()
        return when (stansEllerOpphør?.vurdering) {
            null -> true
            is Stans -> true
            is Opphør -> !løsning.definisjon().måRevurderesEtterOpphør || validerDatoMotKrav(krav, løsning)
        }
    }

    private fun validerDatoMotKrav(krav: KravMedDato, løsning: PeriodisertAvklaringsbehovLøsning<*>): Boolean {
        return løsning.løsningerForPerioder.any { it.fom >= krav.muligRettFra }
    }
}