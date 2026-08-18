package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.flate

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.komponenter.verdityper.Beløp
import java.math.BigDecimal
import java.time.LocalDate

data class Avslag11_27VurderingDto(
    val referanse: String,
    val begrunnelse: String,
    val harAnnenFullYtelse: Boolean,
    val brukersYtelse: Ytelse? = null,
    val brukersYtelseTom: LocalDate? = null,
    /*  Kun for sykepenger */
    val sykepengegrunnlag: Beløp? = null,
    val harArbeidsgiverSykepengerUtbetaling: Boolean? = null,
    val skalAvslås1127: Boolean? = null,
)
