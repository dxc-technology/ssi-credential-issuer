package com.luxoft.ssi.web.data

import kotlin.math.absoluteValue
import kotlin.random.Random

data class IndySchema(
    val name: String,
    val attributes: AttributesWithDefaultValueMap,
    val version: String
) {
    constructor(name: String, attributes: Map<String, String>, version: String) : this(
        name,
        AttributesWithDefaultValueMap(attributes),
        version
    )

    //This is required because objects hash is used as ID, but we don`t wont it to change because of different default values
    class AttributesWithDefaultValueMap(map: Map<String, String>) : HashMap<String, String>() {
        init {
            putAll(map)
        }

        override fun hashCode(): Int {
            return keys.hashCode()
        }
    }
}

object PossibleIndySchemas {
    object Human {
        val v1_0 = IndySchema(
            name = "Human",
            attributes = mapOf("name" to "John", "age" to "18", "sex" to "male", "height" to "180"),
            version = "1.0"
        )

        val v1_1 = IndySchema(
            name = "Human",
            version = "1.1",
            attributes = mapOf(
                "name" to "John",
                "age" to "18",
                "sex" to "male",
                "height" to "180",
                "profile picture" to "-"
            )
        )
    }

    object Person {
        val v1_0 = IndySchema(
            name = "Person",
            version = "1.0",
            attributes = mapOf(
                "socialid" to "0123456789",
                "name" to "John Doe",
                "birthday" to "1974-01-01",
                "gender" to "male",
                "picture" to "-"
            )
        )
    }

    object Insurance {
        val v1_0 = IndySchema(
            name = "Insurance",
            version = "1.0",
            attributes = mapOf(
                "medicalid" to "0123456789",
                "insurer" to "Techniker Krankenkasse TK",
                "limit" to "100000$",
                "partners" to "TC SEEHOF, Marina Bay Hospital"
            )
        )
    }

    object Prescription{
        val v1_0 = IndySchema(
            name = "Prescription",
            version = "1.0",
            attributes = mapOf(
                "diagnosis" to "Leukemia",
                "prescription" to "Santorium"
            )
        )
    }

    object Patient {
        val v1_0 = IndySchema(
            name = "Patient",
            attributes = mapOf("medical id" to "${Random.nextInt().absoluteValue}", "medical condition" to "Healthy"),
            version = "1.0"
        )
    }

    object Citizen {
        val v1_0 = IndySchema(
            name = "Citizen",
            attributes = mapOf("government id" to "${Random.nextInt().absoluteValue}", "address" to "Russia, St. Petersburg, Lenina st. 1"),
            version = "1.0"
        )
    }

    val active = listOf(Human.v1_0, Patient.v1_0, Citizen.v1_0, Human.v1_1, Person.v1_0, Insurance.v1_0, Prescription.v1_0)
}
