package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDate

interface LovvalgDelvurderingRepository : Repository {
    /** Alle lagrede delvurderinger for behandlingen, sortert på [LovvalgDelvurderingerGrunnlag.relevantDato]. */
    fun hentAlle(behandlingId: BehandlingId): List<LovvalgDelvurderingerGrunnlag>

    /** Delvurderingen for den nyeste relevante datoen. */
    fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgDelvurderingerGrunnlag? =
        hentAlle(behandlingId).maxByOrNull { it.relevantDato }

    /** Erstatter hele settet med delvurderinger. Det skrives ikke ny historikk dersom settet er uendret. */
    fun lagre(behandlingId: BehandlingId, delvurderinger: List<LovvalgDelvurderingerGrunnlag>)

    fun lagre(behandlingId: BehandlingId, vurdering: KanBehandlesAutomatiskVurdering, relevantDato: LocalDate) =
        lagre(
            behandlingId,
            listOf(
                LovvalgDelvurderingerGrunnlag(
                    kanBehandlesAutomatisk = vurdering.kanBehandlesAutomatisk,
                    tilhørighetVurdering = vurdering.tilhørighetVurdering,
                    relevantDato = relevantDato,
                )
            )
        )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
