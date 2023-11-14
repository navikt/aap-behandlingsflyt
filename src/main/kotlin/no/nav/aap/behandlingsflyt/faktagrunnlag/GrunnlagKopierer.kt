package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository

class GrunnlagKopierer(connection: DBConnection) {

    private val vilkårsresultatRepository: VilkårsresultatRepository = VilkårsresultatRepository(connection)
    private val personopplysningRepository = PersonopplysningRepository(connection)
    private val bistandRepository = BistandRepository(connection)
    private val sykdomsRepository = SykdomsRepository(connection)

    fun overfør(fraBehandling: Behandling, tilBehandling: Behandling) {
        vilkårsresultatRepository.kopier(fraBehandling, tilBehandling)
        personopplysningRepository.kopier(fraBehandling.id, tilBehandling.id)
        YrkesskadeRepository.kopier(fraBehandling, tilBehandling)
        sykdomsRepository.kopier(fraBehandling, tilBehandling)
        InMemoryStudentRepository.kopier(fraBehandling, tilBehandling)
        bistandRepository.kopier(fraBehandling.id, tilBehandling.id)
        SykepengerErstatningRepository.kopier(fraBehandling, tilBehandling)
    }
}
