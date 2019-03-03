package urlshortener

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object UrlDatabase {

    /**
     * Database that holds information needed for redirects
     */
    object UrlTable : Table() {
        val id = integer("id").autoIncrement().primaryKey()
        val key = varchar("key", 10)
        val url = varchar("url", 1000)
    }

    /**
     * Data object that actually holds values for [UrlTable]
     */
    data class UrlObject(val id: Int, val key: String, val url: String)

    fun init() {
        Database.connect("jdbc:sqlite:urldatabase.db", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.create(UrlTable)
        }
    }

    /**
     * Creates empty url to be populated later
     */
    fun createUrl(): Int {
        return transaction {
            val id = UrlTable.insert {
                it[key] = ""
                it[url] = ""
            } get UrlTable.id
            id!!
        }
    }

    fun updateUrl(id: Int, key: String, url: String) {
        if (id == -1) throw Exception("ID cannot be null for url on update")
        transaction {
            UrlTable.update({ UrlTable.id eq id }) {
                it[UrlTable.key] = key
                it[UrlTable.url] = url
            }
        }
    }

    fun getUrl(key: String): UrlObject? {
        return transaction { UrlTable.select { UrlTable.key.eq(key) }.firstOrNull()?.toUrlObject() }
    }

    fun doesUrlExist(key: String): Boolean {
        return transaction { UrlTable.select { UrlTable.key.eq(key) }.count() > 0 }
    }

    fun getUrlById(id: Int): UrlObject? {
        return transaction { UrlTable.select { UrlTable.id.eq(id) }.firstOrNull()?.toUrlObject() }
    }
}

fun ResultRow.toUrlObject() =
    UrlDatabase.UrlObject(this[UrlDatabase.UrlTable.id], this[UrlDatabase.UrlTable.key], this[UrlDatabase.UrlTable.url])