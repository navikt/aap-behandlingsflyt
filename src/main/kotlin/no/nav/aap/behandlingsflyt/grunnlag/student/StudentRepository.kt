package no.nav.aap.behandlingsflyt.grunnlag.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.grunnlag.student.StudentGrunnlag

interface StudentRepository {
    fun lagre(behandlingId: Long, studentvurdering: StudentVurdering?)
    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling)
    fun hentHvisEksisterer(behandlingId: Long): StudentGrunnlag?
    fun hent(behandlingId: Long): StudentGrunnlag
}