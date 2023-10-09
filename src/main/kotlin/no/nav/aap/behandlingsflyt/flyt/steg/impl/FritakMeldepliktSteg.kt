package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class FritakMeldepliktSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val avklaringsbehovene = behandling.avklaringsbehovene()
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FRITAK_MELDEPLIKT)
        if (avklaringsbehov == null) {
            // Legger til et ferdig løst behov så saksbehandler alltid har muligheten til å legge inn en vurdering
            avklaringsbehovene.leggTil(
                Avklaringsbehov(
                    definisjon = Definisjon.FRITAK_MELDEPLIKT,
                    historikk = mutableListOf(
                        Endring(
                            status = Status.AVSLUTTET,
                            begrunnelse = "",
                            endretAv = "system"
                        )
                    ),
                    funnetISteg = behandling.aktivtSteg()
                )
            )
        }

        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.FRITAK_MELDEPLIKT
    }
}
