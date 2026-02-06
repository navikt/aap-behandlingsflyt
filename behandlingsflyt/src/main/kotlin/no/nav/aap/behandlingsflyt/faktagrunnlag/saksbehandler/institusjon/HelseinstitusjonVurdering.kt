package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class HelseinstitusjonVurdering(
    val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean? = null,
    val harFasteUtgifter: Boolean? = null,
    val periode: Periode,
    val vurdertIBehandling: BehandlingId,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime
)