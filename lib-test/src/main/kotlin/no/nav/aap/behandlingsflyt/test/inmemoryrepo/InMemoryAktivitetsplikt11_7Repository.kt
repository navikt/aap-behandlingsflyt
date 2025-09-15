package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Varsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

object InMemoryAktivitetsplikt11_7Repository : Aktivitetsplikt11_7Repository {

    private val memory = HashMap<BehandlingId, List<Aktivitetsplikt11_7Vurdering>>()
    private val varselMemory = HashMap<BehandlingId, Aktivitetsplikt11_7Varsel>()
    private val lock = Object()

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<Aktivitetsplikt11_7Vurdering>
    ) {
        synchronized(lock) {
            memory.put(behandlingId, vurderinger)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        synchronized(lock) {
            memory.put(tilBehandling, memory.getValue(fraBehandling))
        }
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Grunnlag? {
        return synchronized(lock) {
            memory[behandlingId]?.let { Aktivitetsplikt11_7Grunnlag(it) }
        }
    }


    override fun lagreVarsel(
        behandlingId: BehandlingId,
        varsel: BrevbestillingReferanse
    ) {
        synchronized(lock) {
            varselMemory.put(behandlingId, Aktivitetsplikt11_7Varsel(varselId = varsel))
        }
    }

    override fun lagreFrist(
        behandlingId: BehandlingId,
        datoVarslet: LocalDate,
        svarfrist: LocalDate
    ) {
        synchronized(lock) {
            val oppdatertVarsel = varselMemory[behandlingId]?.copy(
                svarfrist = svarfrist,
                sendtDato = datoVarslet
            ) ?: error("Fant ikke varsel for $behandlingId")
            varselMemory.put(behandlingId, oppdatertVarsel)
        }
    }

    override fun hentVarselHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Varsel? {
        return synchronized(lock) {
            varselMemory[behandlingId]
        }
    }
}