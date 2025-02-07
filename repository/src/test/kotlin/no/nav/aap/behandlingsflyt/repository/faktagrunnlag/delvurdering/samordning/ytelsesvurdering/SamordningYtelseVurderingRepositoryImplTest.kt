package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SamordningYtelseVurderingRepositoryImplTest {
    @Test
    fun `fsdf sd`() {
        InitTestDatabase.dataSource.transaction {
            SamordningYtelseVurderingRepositoryImpl(it).lagreYtelser(
                behandlingId = BehandlingId(0),
                samordningYtelser = listOf(

                )
            )
        }
    }
}