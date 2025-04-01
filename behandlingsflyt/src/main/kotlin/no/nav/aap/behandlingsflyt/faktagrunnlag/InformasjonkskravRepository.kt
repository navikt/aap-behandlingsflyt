package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import java.time.Instant

interface InformasjonkskravRepository: Repository {
    fun hentOppdateringer(sakId: SakId, krav: List<InformasjonskravNavn>): List<InformasjonskravOppdatert>
    fun registrerOppdateringer(sakId: SakId, behandlingId: BehandlingId, informasjonskrav: List<InformasjonskravNavn>, oppdatert: Instant)
}