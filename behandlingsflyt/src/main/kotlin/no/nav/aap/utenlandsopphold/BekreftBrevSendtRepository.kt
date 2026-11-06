package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface BekreftBrevSendtRepository : Repository {
    fun lagre(behandlingId: BehandlingId, bekreftelse: BekreftBrevSendtBekreftelse)

    fun hentHvisEksisterer(behandlingId: BehandlingId): BekreftBrevSendtGrunnlag?
}
