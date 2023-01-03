package com.android.vadify.data.db.contact

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.vadify.ui.dashboard.Dashboard


@Entity(tableName = "contacts", inheritSuperIndices = false)
data class Contact(
    @PrimaryKey var name: String,
    var phone: String,
    var imageUri: String?,
    var sortLetter: String?,
    var selection: Boolean = false

) {
    object Mapper {
        fun from(response: Dashboard.ContactInformation): Contact {
            return response.run {
                Contact(
                    name = name,
                    phone = phone,
                    imageUri = imageUri,
                    sortLetter = sortLetter,
                    selection = selection
                )
            }
        }
    }
}

@Dao
interface ContactListCache {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(card: List<Contact>)

    @Query("SELECT * FROM contacts")
    fun getAll(): LiveData<List<Contact>?>

    @Query("DELETE FROM contacts")
    fun deleteAll()

//    @Query("DELETE FROM contacts WHERE id = :cardId")
//    fun delete(cardId: Long)
}
