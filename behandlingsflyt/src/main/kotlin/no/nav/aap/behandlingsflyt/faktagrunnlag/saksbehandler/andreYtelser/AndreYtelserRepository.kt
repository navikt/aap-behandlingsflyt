package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelser

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalinger
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface AndreYtelserRepository : Repository {
    fun lagre(behandlingId: BehandlingId, andreUtbetalinger: AndreUtbetalinger)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): AndreUtbetalinger?
    fun hent(behandlingId: BehandlingId): AndreUtbetalinger
}