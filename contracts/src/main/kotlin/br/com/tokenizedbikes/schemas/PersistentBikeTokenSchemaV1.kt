package br.com.tokenizedbikes.schemas

import br.com.tokenizedbikes.models.BikeColor
import br.com.tokenizedbikes.models.BikeColorEnum
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import java.io.Serializable
import javax.persistence.*

@CordaSerializable
object PersistentBikeTokenSchemaV1 : MappedSchema(
    schemaFamily = PersistentBikeTokenSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentBikeToken::class.java, PersistentBikeColor::class.java)
) {

    @Entity
    @CordaSerializable
    @Table(
        name = "tbl_bike_token_state",
        indexes = [Index(name = "bt_serial_number_idx", columnList = "bt_serial_number")]
    )
    data class PersistentBikeToken(
        var linearId: String? = null,
        @Column(name = "bt_brand")
        var brand: String? = null,
        @Column(name = "bt_model_name")
        var modelName: String? = null,
        @Column(name = "bt_bike_image_url")
        var bikeImageURL: String? = null,
        @Column(name = "bt_serial_number")
        var serialNumber: String? = null,
        @Column(name = "bt_year")
        var year: Int? = null,
        @Column(name = "bt_percent_of_conservation")
        var percentOfConservation: Double? = null,
        @Column(name = "bt_dollar_price")
        var dollarPrice: Double? = null,
        @Column(name = "bt_coin_price")
        var coinPrice: Double? = null,
        @Column(name = "bt_is_new")
        var isNew: Boolean? = null,
        @Column(name = "bt_maintainer_name")
        var maintainer: AbstractParty? = null,
        @OneToOne(cascade = [CascadeType.ALL])
        @JoinColumn(name = "bc_id_fk", referencedColumnName = "bc_id")
        var color: PersistentBikeColor? = null

    ) : PersistentState(), Serializable

    @Entity
    @CordaSerializable
    @Table(name = "tbl_bike_color")
    data class PersistentBikeColor(
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @Column(name = "bc_id")
        var id: Long? = null,
        @Column(name = "bc_main_color")
        var mainColor: BikeColorEnum? = null,
        @ElementCollection
        var otherColors: MutableSet<BikeColorEnum> = mutableSetOf(),
        @Column(name = "bc_is_custom")
        var isCustomColor: Boolean? = null,
        @Column(name = "bc_color_description")
        var colorDescription: String? = null,

        @OneToOne(mappedBy = "color")
        var persistentBikeToken: PersistentBikeToken? = null
    ) : Serializable {
        companion object {
            fun fromBikeColor(bikeColor: BikeColor): PersistentBikeColor {
                return PersistentBikeColor(
                    colorDescription = bikeColor.colorDescription,
                    isCustomColor = bikeColor.isCustomColor,
                    mainColor = bikeColor.mainColor,
                    otherColors = bikeColor.otherColors
                )
            }
        }
    }

}