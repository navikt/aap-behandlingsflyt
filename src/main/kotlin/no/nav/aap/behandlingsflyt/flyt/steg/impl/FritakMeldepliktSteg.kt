package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class FritakMeldepliktSteg(private val behandlingService: BehandlingService) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val behandling = behandlingService.hent(kontekst.behandlingId)

        val avklaringsbehovene = behandling.avklaringsbehovene()
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FRITAK_MELDEPLIKT)
        if (avklaringsbehov == null) {
            // Legger til et ferdig løst behov så saksbehandler alltid har muligheten til å legge inn en vurdering
            avklaringsbehovene.leggTil(
                Avklaringsbehov(
                    id = 1L,
                    definisjon = Definisjon.FRITAK_MELDEPLIKT,
                    historikk = mutableListOf(
                        Endring(
                            status = Status.AVSLUTTET,
                            begrunnelse = "",
                            endretAv = "system"
                        )
                    ),
                    funnetISteg = behandling.aktivtSteg(),
                    kreverToTrinn = null
                )
            )
        }

        return StegResultat()
    }
}
