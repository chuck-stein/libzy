package io.libzy.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genre")
data class DbGenre(

    // TODO: use an auto-generated integer PK and make name a unique field (but first ensure insert collisions still apply to unique fields and not just PKs)
    @PrimaryKey
    @ColumnInfo(name = "name_id")
    val nameId: String
)
