package com.android.vadify.data.db.commands

import androidx.room.*
import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.login.fragment.CommandsSpeech


@Entity(tableName = "commands",primaryKeys = ["language","commandName"], inheritSuperIndices = false)
data class  Commands(
    var language: String,
    var commandName: String,
    var languageCode:String,
    var command1: String?,
    var command2: String?,
    var command3: String?
) {
    object Mapper {
        fun from(response: CommandsFragment.CommandData): Commands {
            return response.run {
                Commands(
                    language = language,
                    commandName = commandName,
                    languageCode = languageCode,
                    command1 = command1,
                    command2 = command2,
                    command3 = command3

                )
            }
        }
    }

}


@Dao
interface CommandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cmd: Commands)


    @Query("SELECT * FROM commands")
    fun getAllData(): List<Commands>

    @Query("SELECT * FROM commands where commandName=:commandName and language=:language")
    fun getAll(commandName: String, language: String): List<Commands>

    @Query("SELECT * FROM commands where language=:language")
    fun getAllChat(language: String): List<Commands>

    @Query("DELETE FROM commands")
    fun deleteAll()

    @Query("UPDATE commands SET command1 = :command1, command2= :command2, command3= :command3 WHERE language =:language and commandName =:commandName")
    fun update(
        command1: String?,
        command2: String?,
        command3: String?,
        language: String,
        commandName: String?
    )

}