package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate


data class PeriodisertForutgåendeMedlemskapGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<PeriodisertManuellVurderingForForutgåendeMedlemskapResponse>,
    override val nyeVurderinger: List<PeriodisertManuellVurderingForForutgåendeMedlemskapResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    val overstyrt: Boolean = false
): PeriodiserteVurderingerDto<PeriodisertManuellVurderingForForutgåendeMedlemskapResponse>

data class PeriodisertManuellVurderingForForutgåendeMedlemskapResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse?,
    override val kvalitetssikretAv: VurdertAvResponse? = null,
    override val besluttetAv: VurdertAvResponse? = null,
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
    val overstyrt: Boolean = false,
): VurderingDto

fun ManuellVurderingForForutgåendeMedlemskap.toResponse(
    vurdertAvService: VurdertAvService,
    fom: LocalDate = this.fom,
    tom: LocalDate? = this.tom,
) =
    PeriodisertManuellVurderingForForutgåendeMedlemskapResponse(
        fom = fom,
        tom = tom,
        vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, vurdertTidspunkt.toLocalDate()),
        besluttetAv = vurdertAvService.besluttetAv(
            definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
            behandlingId = vurdertIBehandling
        ),
        begrunnelse = begrunnelse,
        harForutgåendeMedlemskap = harForutgåendeMedlemskap,
        varMedlemMedNedsattArbeidsevne = varMedlemMedNedsattArbeidsevne,
        medlemMedUnntakAvMaksFemAar = medlemMedUnntakAvMaksFemAar,
        overstyrt = overstyrt
    )