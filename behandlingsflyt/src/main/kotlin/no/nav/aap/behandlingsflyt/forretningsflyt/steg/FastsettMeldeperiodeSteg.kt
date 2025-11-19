package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.MELDEPERIODE_LENGDE
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.DayOfWeek

class FastsettMeldeperiodeSteg(
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        meldeperiodeRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val rettighetsperiode = kontekst.rettighetsperiode
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    return Fullført
                }

                /**
                 * TODO: Hva er en fornuftig lengde for en aktuell periode med meldeperioder?
                 * Hvor langt frem i tid "trenger" man å generere 2-ukers-perioder
                  */

                val aktuellPeriode = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusYears(10))

                oppdaterMeldeperioder(kontekst.behandlingId, aktuellPeriode)
                return Fullført
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                return Fullført
            }
        }
    }

    fun oppdaterMeldeperioder(behandlingId: BehandlingId, rettighetsperiode: Periode) {
        val gamlePerioder = meldeperiodeRepository.hent(behandlingId)

        val meldeperioder = utledMeldeperiode(gamlePerioder, rettighetsperiode)

        if (meldeperioder != gamlePerioder) {
            meldeperiodeRepository.lagre(behandlingId, meldeperioder)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FastsettMeldeperiodeSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_MELDEPERIODER
        }

        fun utledMeldeperiode(
            gamlePerioder: List<Periode>,
            aktuellTidsperiode: Periode
        ): List<Periode> {
            val fastsattDag = gamlePerioder.firstOrNull()?.fom

            val førsteFastsatteDag = if (fastsattDag == null)
                generateSequence(aktuellTidsperiode.fom) { it.minusDays(1) }
                    .first { it.dayOfWeek == DayOfWeek.MONDAY }
            else
                generateSequence(fastsattDag) { it.minusDays(MELDEPERIODE_LENGDE) }
                    .first { it <= aktuellTidsperiode.fom }

            return generateSequence(førsteFastsatteDag) { it.plusDays(MELDEPERIODE_LENGDE) }
                .takeWhile { it <= aktuellTidsperiode.tom }
                .map { Periode(it, it.plusDays(MELDEPERIODE_LENGDE - 1)) }
                .toList()
        }
    }
}