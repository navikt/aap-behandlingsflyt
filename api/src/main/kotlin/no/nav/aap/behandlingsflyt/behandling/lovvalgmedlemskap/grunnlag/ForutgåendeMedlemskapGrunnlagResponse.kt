package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import java.time.LocalDate

data class ForutgåendeMedlemskapGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: ManuellVurderingForForutgåendeMedlemskapResponse?,
    val historiskeManuelleVurderinger: List<HistoriskManuellVurderingForForutgåendeMedlemskapResponse>
)

data class ManuellVurderingForForutgåendeMedlemskapResponse(
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
    val vurdertAv: VurdertAvResponse,
    val overstyrt: Boolean = false
)

data class HistoriskManuellVurderingForForutgåendeMedlemskapResponse(
    val manuellVurdering: ManuellVurderingForForutgåendeMedlemskapResponse,
    val opprettet: LocalDate
)

fun ManuellVurderingForForutgåendeMedlemskap.toResponse(): ManuellVurderingForForutgåendeMedlemskapResponse =
    ManuellVurderingForForutgåendeMedlemskapResponse(
        begrunnelse = begrunnelse,
        harForutgåendeMedlemskap = harForutgåendeMedlemskap,
        varMedlemMedNedsattArbeidsevne = varMedlemMedNedsattArbeidsevne,
        medlemMedUnntakAvMaksFemAar = medlemMedUnntakAvMaksFemAar,
        vurdertAv =
            VurdertAvResponse(
                ident = vurdertAv,
                dato =
                    vurdertTidspunkt?.toLocalDate()
                        ?: error("Mangler vurdertDato på ManuellVurderingForForutgåendeMedlemskap")
            ),
        overstyrt = overstyrt
    )

fun HistoriskManuellVurderingForForutgåendeMedlemskap.toResponse(): HistoriskManuellVurderingForForutgåendeMedlemskapResponse =
    HistoriskManuellVurderingForForutgåendeMedlemskapResponse(
        manuellVurdering = manuellVurdering.toResponse(),
        opprettet = opprettet.toLocalDate()
    )