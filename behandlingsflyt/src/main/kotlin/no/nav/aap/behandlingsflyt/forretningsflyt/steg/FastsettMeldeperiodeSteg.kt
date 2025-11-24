package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeUtleder.utledMeldeperiode
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettMeldeperiodeSteg(
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val sakRepository: SakRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        meldeperiodeRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        sakRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    return Fullført
                }

                val aktuellPeriode = sakRepository.hent(kontekst.sakId).rettighetsperiodeEttÅrFraStartDato()
                oppdaterFørsteMeldeperiode(kontekst.behandlingId, aktuellPeriode)
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

    fun oppdaterFørsteMeldeperiode(behandlingId: BehandlingId, aktuellPeriode: Periode) {
        val førsteMeldeperiode = meldeperiodeRepository.hentFørsteMeldeperiode(behandlingId)

        val startdatoForrigeMeldeperiode = førsteMeldeperiode?.fom
        val meldeperioder = utledMeldeperiode(startdatoForrigeMeldeperiode, aktuellPeriode)

        val startdatoNyMeldeperiode = meldeperioder.first().fom
        if (startdatoNyMeldeperiode != startdatoForrigeMeldeperiode) {
            meldeperiodeRepository.lagreFørsteMeldeperiode(behandlingId, meldeperioder.first())
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

    }
}