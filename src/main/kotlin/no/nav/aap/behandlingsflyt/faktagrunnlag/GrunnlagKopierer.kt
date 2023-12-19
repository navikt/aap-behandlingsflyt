package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository

class GrunnlagKopierer(connection: DBConnection) {

    private val vilkårsresultatRepository: VilkårsresultatRepository = VilkårsresultatRepository(connection)
    private val personopplysningRepository = PersonopplysningRepository(connection)
    private val yrkesskadeRepository = YrkesskadeRepository(connection)
    private val sykdomRepository = SykdomRepository(connection)
    private val studentRepository = StudentRepository(connection)
    private val bistandRepository = BistandRepository(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)
    private val sykepengerErstatningRepository = SykepengerErstatningRepository(connection)
    private val arbeidsevneRepository = ArbeidsevneRepository(connection)
    private val uføreRepository = UføreRepository(connection)
    private val pliktkortRepository = PliktkortRepository(connection)

    fun overfør(fraBehandling: Behandling, tilBehandling: Behandling) {
        vilkårsresultatRepository.kopier(fraBehandling.id, tilBehandling.id)
        personopplysningRepository.kopier(fraBehandling.id, tilBehandling.id)
        yrkesskadeRepository.kopier(fraBehandling.id, tilBehandling.id)
        sykdomRepository.kopier(fraBehandling.id, tilBehandling.id)
        studentRepository.kopier(fraBehandling.id, tilBehandling.id)
        bistandRepository.kopier(fraBehandling.id, tilBehandling.id)
        meldepliktRepository.kopier(fraBehandling.id, tilBehandling.id)
        sykepengerErstatningRepository.kopier(fraBehandling.id, tilBehandling.id)
        arbeidsevneRepository.kopier(fraBehandling.id, tilBehandling.id)
        uføreRepository.kopier(fraBehandling.id, tilBehandling.id)
        pliktkortRepository.kopier(fraBehandling.id, tilBehandling.id)
    }
}
