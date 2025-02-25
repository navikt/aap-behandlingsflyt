package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling

interface BehandlingHendelseService {
    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene, typeBrev: TypeBrev? = null)
}