package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDateTime

data class HelseinstitusjonVurdering(
    val periode: Periode,
    val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean? = null,
    val harFasteUtgifter: Boolean? = null,
    val erHistoriskUtenReduksjonsberegning: Boolean = false,
    val vurdertIBehandling: BehandlingId,
    val vurdertAv: Bruker?,
    val vurdertTidspunkt: LocalDateTime?,
    val oppholdId: String? = null,
)