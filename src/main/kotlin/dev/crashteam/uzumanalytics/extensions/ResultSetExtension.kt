package dev.crashteam.uzumanalytics.extensions

import java.math.BigDecimal
import java.sql.ResultSet

fun ResultSet.getNullableBigDecimal(columnName: String): BigDecimal? {
    val value = this.getString(columnName)
    return (if (this.wasNull()) null else value.toBigDecimal())
}
