package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface YrkesskadeRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag?
    fun lagre(behandlingId: BehandlingId, registerYrkesskader: Yrkesskader?, oppgittYrkesskadeISøknad: Boolean?)
    fun hentKandidaterForBackfill(): List<BackfillKandidat>
    fun backfillYrkesskadeDato(yrkesskadeDatoId: Long, yrkesskade: Yrkesskade)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}

data class BackfillKandidat(
    val yrkesskadeDatoId: Long,
    val behandlingId: BehandlingId,
    val ref: String,
    val saksnummer: Int?,
    val kildesystem: String,
    val skadedato: LocalDate?,
)