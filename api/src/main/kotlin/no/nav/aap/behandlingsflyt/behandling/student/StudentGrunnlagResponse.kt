package no.nav.aap.behandlingsflyt.behandling.student

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import java.time.LocalDate

open class StudentGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val studentvurdering: StudentVurderingResponse?,
    val oppgittStudent: OppgittStudent?,
)

data class StudentVurderingResponse(
    val id: Long? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
    val vurdertAv: VurdertAvResponse,
)

