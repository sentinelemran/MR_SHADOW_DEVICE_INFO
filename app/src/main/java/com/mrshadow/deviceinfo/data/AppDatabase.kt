package com.mrshadow.deviceinfo

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.gson.annotations.SerializedName
import com.mrshadow.deviceinfo.network.ReportApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Entity(tableName = "app_links")
data class AppLinkEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val downloadUrl: String?,
    val websiteUrl: String?,
    val version: String?,
    val visible: Boolean = true,
    val position: Int = 0
)

@Dao
interface AppLinkDao {
    @Query("SELECT * FROM app_links ORDER BY position ASC") suspend fun all(): List<AppLinkEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(item: AppLinkEntity)
    @Query("DELETE FROM app_links WHERE id = :id") suspend fun remove(id: String)
}

@Database(entities = [AppLinkEntity::class], version = 1, exportSchema = false)
abstract class ShadowDatabase : RoomDatabase() {
    abstract fun appLinkDao(): AppLinkDao
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): ShadowDatabase =
        Room.databaseBuilder(context, ShadowDatabase::class.java, "shadow-device-info.db").build()

    @Provides fun appLinkDao(db: ShadowDatabase): AppLinkDao = db.appLinkDao()

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides @Singleton
    fun reportApi(client: OkHttpClient): ReportApi? {
        val base = BuildConfig.REPORT_API_BASE_URL
        if (base.isBlank() || !base.startsWith("https://")) return null
        return Retrofit.Builder().baseUrl(if (base.endsWith("/")) base else "$base/")
            .client(client).addConverterFactory(GsonConverterFactory.create()).build()
            .create(ReportApi::class.java)
    }
}
