package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import java.time.LocalDate

class SykdomsFaktagrunnlag(
    val typeBehandling: TypeBehandling,
    val kravDato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykepengerErstatningFaktagrunnlag: SykepengerErstatningGrunnlag?,
    val sykdomsvurderinger: List<Sykdomsvurdering>,
    val bistandvurderingFaktagrunnlag: BistandGrunnlag?,
    val studentvurdering: StudentVurdering?
) : Faktagrunnlag