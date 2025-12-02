package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.komponenter.type.Periode

class SykepengerErstatningFaktagrunnlag(
    val rettighetsperiode: Periode,
    val sykepengeerstatningGrunnlag: SykepengerErstatningGrunnlag?,
    val sykdomGrunnlag: SykdomGrunnlag?,
) : Faktagrunnlag
