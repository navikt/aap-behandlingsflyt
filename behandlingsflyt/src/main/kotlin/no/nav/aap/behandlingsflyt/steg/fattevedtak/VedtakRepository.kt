package no.nav.aap.behandlingsflyt.steg.fattevedtak

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface VedtakRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vedtakstidspunkt: LocalDateTime, virkningstidspunkt: LocalDate?)
    fun hent(behandlingId: BehandlingId): Vedtak?
    fun hentId(behandlingId: BehandlingId): Long
}