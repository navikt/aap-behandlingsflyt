package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Helseoppholdvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Soningsvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

object InMemoryInstitusjonsoppholdRepository : InstitusjonsoppholdRepository {

    private val idSeq = AtomicLong(1)
    private val memory = HashMap<BehandlingId, InstitusjonsoppholdGrunnlag>()
    private val lock = Any()

    fun reset() = synchronized(lock) { memory.clear() }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? =
        synchronized(lock) { memory[behandlingId] }

    override fun hent(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag =
        synchronized(lock) { requireNotNull(memory[behandlingId]) { "Fant ikke InstitusjonsoppholdGrunnlag for $behandlingId" } }

    override fun lagreOpphold(behandlingId: BehandlingId, institusjonsopphold: List<Institusjonsopphold>) =
        synchronized(lock) {
            val eksisterende = memory[behandlingId] ?: InstitusjonsoppholdGrunnlag()
            memory[behandlingId] = eksisterende.copy(
                oppholdene = Oppholdene(idSeq.andIncrement, institusjonsopphold.map { it.tilInstitusjonSegment() })
            )
        }

    override fun lagreSoningsVurdering(
        behandlingId: BehandlingId,
        vurdertAv: String,
        soningsvurderinger: List<Soningsvurdering>
    ) = synchronized(lock) {
        val eksisterende = memory[behandlingId] ?: InstitusjonsoppholdGrunnlag()
        memory[behandlingId] = eksisterende.copy(
            soningsVurderinger = Soningsvurderinger(
                vurdertAv = vurdertAv,
                vurdertTidspunkt = LocalDateTime.now(),
                vurderinger = soningsvurderinger
            )
        )
    }

    override fun lagreHelseVurdering(
        behandlingId: BehandlingId,
        helseinstitusjonVurderinger: List<HelseinstitusjonVurdering>
    ) = synchronized(lock) {
        val eksisterende = memory[behandlingId] ?: InstitusjonsoppholdGrunnlag()
        memory[behandlingId] = eksisterende.copy(
            helseoppholdvurderinger = Helseoppholdvurderinger(
                vurderinger = helseinstitusjonVurderinger,
                id = idSeq.getAndIncrement(),
                vurdertTidspunkt = LocalDateTime.now(),
            )
        )
    }

    override fun hentVurderingerGruppertPerOpphold(behandlingId: BehandlingId): Map<Periode, List<HelseinstitusjonVurdering>> =
        synchronized(lock) {
            memory[behandlingId]?.helseoppholdvurderinger?.vurderinger
                ?.groupBy { it.periode }
                ?: emptyMap()
        }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId): Unit =
        synchronized(lock) {
            memory[fraBehandling]?.let { memory[tilBehandling] = it }
        }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.remove(behandlingId)
        }
    }
}