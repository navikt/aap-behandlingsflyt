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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.DayOfWeek

class FastsettMeldeperiodeSteg(
    private val sakRepository: SakRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sakRepository = repositoryProvider.provide(),
        meldeperiodeRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    return Fullført
                }

                val rettighetsperiode = sakRepository.hent(kontekst.sakId).rettighetsperiode
                oppdaterMeldeperioder(kontekst.behandlingId, rettighetsperiode)
                return Fullført
            }

            VurderingType.REVURDERING -> {
                val rettighetsperiode = sakRepository.hent(kontekst.sakId).rettighetsperiode
                oppdaterMeldeperioder(kontekst.behandlingId, rettighetsperiode)
                return Fullført
            }

            VurderingType.FORLENGELSE,
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
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return FastsettMeldeperiodeSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_MELDEPERIODER
        }

        fun utledMeldeperiode(
            gamlePerioder: List<Periode>,
            rettighetsperiode: Periode
        ): List<Periode> {
            val fastsattDag = gamlePerioder.firstOrNull()?.fom

            val førsteFastsatteDag = if (fastsattDag == null)
                generateSequence(rettighetsperiode.fom) { it.minusDays(1) }
                    .first { it.dayOfWeek == DayOfWeek.MONDAY }
            else
                generateSequence(fastsattDag) { it.minusDays(MELDEPERIODE_LENGDE) }
                    .first { it <= rettighetsperiode.fom }

            return generateSequence(førsteFastsatteDag) { it.plusDays(MELDEPERIODE_LENGDE) }
                .takeWhile { it <= rettighetsperiode.tom }
                .map { Periode(it, it.plusDays(MELDEPERIODE_LENGDE - 1)) }
                .toList()
        }
    }
}