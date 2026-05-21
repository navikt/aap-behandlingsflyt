package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.BackfillKandidat
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryYrkesskadeRepository : YrkesskadeRepository {

    private val memory = ConcurrentHashMap<BehandlingId, YrkesskadeGrunnlag>()
    private val idSeq = AtomicLong(1)
    private val yrkesskadeDatoIdSeq = AtomicLong(1)
    private val yrkesskadeDatoIndex = ConcurrentHashMap<Long, Pair<BehandlingId, Int>>() // datoId -> (behandlingId, index i yrkesskader-lista)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? =
        memory[behandlingId]

    override fun lagre(behandlingId: BehandlingId, registerYrkesskader: Yrkesskader?, oppgittYrkesskadeISøknad: Boolean?) {
        val existing = memory[behandlingId]
        val nyeYrkesskader = registerYrkesskader ?: Yrkesskader(emptyList())

        // Tildel nye datoId-er for nye yrkesskader og oppdater indeks
        yrkesskadeDatoIndex.entries.removeIf { it.value.first == behandlingId }
        nyeYrkesskader.yrkesskader.forEachIndexed { index, _ ->
            val datoId = yrkesskadeDatoIdSeq.getAndIncrement()
            yrkesskadeDatoIndex[datoId] = behandlingId to index
        }

        memory[behandlingId] = YrkesskadeGrunnlag(
            id = existing?.id ?: idSeq.getAndIncrement(),
            behandlingId = behandlingId,
            yrkesskader = nyeYrkesskader,
            oppgittYrkesskadeISøknad = oppgittYrkesskadeISøknad,
        )
    }

    override fun hentKandidaterForBackfill(): List<BackfillKandidat> =
        yrkesskadeDatoIndex.entries.mapNotNull { (datoId, pair) ->
            val (behandlingId, index) = pair
            val grunnlag = memory[behandlingId] ?: return@mapNotNull null
            val yrkesskade = grunnlag.yrkesskader.yrkesskader.getOrNull(index) ?: return@mapNotNull null
            if (yrkesskade.skadeart != null) return@mapNotNull null
            BackfillKandidat(
                yrkesskadeDatoId = datoId,
                behandlingId = behandlingId,
                ref = yrkesskade.ref,
                saksnummer = yrkesskade.saksnummer,
                kildesystem = yrkesskade.kildesystem,
                skadedato = yrkesskade.skadedato,
            )
        }

    override fun backfillYrkesskadeDato(yrkesskadeDatoId: Long, yrkesskade: Yrkesskade) {
        val (behandlingId, index) = yrkesskadeDatoIndex[yrkesskadeDatoId] ?: return
        val grunnlag = memory[behandlingId] ?: return
        val oppdatert = grunnlag.yrkesskader.yrkesskader.toMutableList()
        val eksisterende = oppdatert.getOrNull(index) ?: return
        oppdatert[index] = eksisterende.copy(
            vedtaksdato = yrkesskade.vedtaksdato,
            skadeart = yrkesskade.skadeart,
            diagnose = yrkesskade.diagnose,
            skadekombinasjoner = yrkesskade.skadekombinasjoner,
            skadekombinasjonerTekst = yrkesskade.skadekombinasjonerTekst,
        )
        memory[behandlingId] = grunnlag.copy(yrkesskader = Yrkesskader(oppdatert))
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        memory[fraBehandling]?.let { grunnlag ->
            val nyGrunnlagId = idSeq.getAndIncrement()
            memory[tilBehandling] = grunnlag.copy(
                id = nyGrunnlagId,
                behandlingId = tilBehandling,
            )
            // Kopier datoId-indekser for den nye behandlingen
            yrkesskadeDatoIndex.entries
                .filter { it.value.first == fraBehandling }
                .forEach { (_, pair) ->
                    val (_, index) = pair
                    val nyDatoId = yrkesskadeDatoIdSeq.getAndIncrement()
                    yrkesskadeDatoIndex[nyDatoId] = tilBehandling to index
                }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        memory.remove(behandlingId)
        yrkesskadeDatoIndex.entries.removeIf { it.value.first == behandlingId }
    }
}
