package io.libzy.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genre")
data class DbGenre(

    // TODO: use an auto-generated integer PK and make name a unique field (but first ensure insert collisions still apply to unique fields and not just PKs)
    @PrimaryKey
    @ColumnInfo(name = "name_id")
    val nameId: String

    // TODO: save this data
//    @ColumnInfo(name = "is_favorite")
//    val isFavorite: Boolean,
//
//    @ColumnInfo(name = "is_recent")
//    val isRecent: Boolean

    // TODO: add genre properties related to audio features -- determine what generalizations can be made from genres to specific features (or combinations of features)

)