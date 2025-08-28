package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import java.time.LocalDate

class SykdomsFaktagrunnlag(
    val typeBehandling: TypeBehandling,
    val kravDato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykepengerErstatningFaktagrunnlag: SykepengerVurdering?,
    val sykdomsvurderinger: List<Sykdomsvurdering>,
    val studentvurdering: StudentVurdering?,
    val overgangArbeidVurdering: List<OvergangArbeidVurdering>?,
    val overgangUføreVurdering: List<OvergangUføreVurdering>?
) : Faktagrunnlag
