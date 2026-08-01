package no.nav.aap.institusjonsopphold

import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker

data class HelseinstitusjonVurdering(
    val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean? = null,
    val harFasteUtgifter: Boolean? = null,
    val periode: Periode,
    val vurdertIBehandling: BehandlingId,
    val vurdertAv: Bruker? = null,
    val vurdertTidspunkt: LocalDateTime?
)