package no.nav.aap.behandlingsflyt.faktagrunnlag.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling

interface StudentRepository {
    fun lagre(behandlingId: Long, studentvurdering: StudentVurdering?)
    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling)
    fun hentHvisEksisterer(behandlingId: Long): StudentGrunnlag?
    fun hent(behandlingId: Long): StudentGrunnlag
}