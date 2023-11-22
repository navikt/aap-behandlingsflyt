package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class FritakMeldepliktSteg private constructor(
    private val behandlingService: BehandlingService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val behandling = behandlingService.hent(kontekst.behandlingId)

        val avklaringsbehovene = avklaringsbehovRepository.hent(kontekst.behandlingId)
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

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FritakMeldepliktSteg(
                BehandlingService(connection),
                AvklaringsbehovRepository(connection))
        }

        override fun type(): StegType {
            return StegType.FRITAK_MELDEPLIKT
        }
    }
}
