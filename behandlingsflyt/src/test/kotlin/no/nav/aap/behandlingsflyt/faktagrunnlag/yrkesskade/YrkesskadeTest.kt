package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

class YrkesskadeTest {
    /*
        @Test
        fun `Yrkesskadedata er oppdatert`() {
            val yrkesskadedatalager = Yrkesskadedatalager()
            val yrkesskade = Yrkesskade(
                datalager = yrkesskadedatalager,
                service = YrkesskadeService()
            )

            yrkesskadedatalager.lagre(Yrkesskadedata(), LocalDateTime.now())

            val erOppdatert = yrkesskade.oppdaterYrkesskade()

            assertThat(erOppdatert).isTrue()
        }

        @Test
        fun `Yrkesskadedata er ikke oppdatert`() {
            val yrkesskadedatalager = Yrkesskadedatalager()
            val yrkesskade = Yrkesskade(
                datalager = yrkesskadedatalager,
                service = YrkesskadeService()
            )

            val erOppdatert = yrkesskade.oppdaterYrkesskade()

            assertThat(erOppdatert).isFalse()
        }

        @Test
        fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
            val yrkesskadedatalager = Yrkesskadedatalager()
            val yrkesskade = Yrkesskade(
                datalager = yrkesskadedatalager,
                service = YrkesskadeService()
            )

            yrkesskadedatalager.lagre(Yrkesskadedata(), LocalDateTime.now().minusDays(1))

            val erOppdatert = yrkesskade.oppdaterYrkesskade()

            assertThat(erOppdatert).isTrue()
        }

        @Test
        fun `Henter yrkesskade fra datalager`() {
            val yrkesskadedatalager = Yrkesskadedatalager()
            val yrkesskade = Yrkesskade(
                datalager = yrkesskadedatalager,
                service = YrkesskadeService()
            )

            val dataFørLagring = yrkesskade.hentYrkesskade()

            assertThat(dataFørLagring).isNull()

            yrkesskadedatalager.lagre(Yrkesskadedata(), LocalDateTime.now().minusDays(1))

            val erOppdatert = yrkesskade.hentYrkesskade()

            assertThat(erOppdatert).isEqualTo(Yrkesskadedata())
        }

        @Test
        fun `Oppdater og hente yrkesskade i liste over grunnlag`() {
            val yrkesskadedatalager = Yrkesskadedatalager()
            val yrkesskade = Yrkesskade(
                datalager = yrkesskadedatalager,
                service = YrkesskadeService()
            )

            val dataFørLagring =Yrkesskade.hentGrunnlag(listOf(yrkesskade))

            assertThat(dataFørLagring).isNull()

            Yrkesskade.oppdater(listOf(yrkesskade))

            val erOppdatert = Yrkesskade.hentGrunnlag(listOf(yrkesskade))

            assertThat(erOppdatert).isEqualTo(Yrkesskadedata())
        }

     */
}
