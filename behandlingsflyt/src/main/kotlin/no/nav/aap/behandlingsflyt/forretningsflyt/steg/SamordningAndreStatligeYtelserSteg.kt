package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SamordningAndreStatligeYtelserSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {}
            VurderingType.REVURDERING -> {
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

                if (erManueltTriggetRevurdering(kontekst, avklaringsbehovene)) {
                    return FantAvklaringsbehov(Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER)
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
            }
        }

        return Fullført
    }

    private fun erManueltTriggetRevurdering(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        return !avklaringsbehovene.erVurdertTidligereIBehandlingen(Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER) &&
                kontekst.erRevurderingMedVurderingsbehov(Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_STATLIGE_YTELSER)
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SamordningAndreStatligeYtelserSteg(repositoryProvider.provide())
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_ANDRE_STATLIGE_YTELSER
        }
    }
}
