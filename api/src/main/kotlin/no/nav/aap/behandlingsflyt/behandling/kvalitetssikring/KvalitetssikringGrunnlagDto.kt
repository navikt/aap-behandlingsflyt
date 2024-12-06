package no.nav.aap.behandlingsflyt.behandling.kvalitetssikring

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering

data class KvalitetssikringGrunnlagDto(val vurderinger: List<TotrinnsVurdering>, val historikk: List<Historikk>)
