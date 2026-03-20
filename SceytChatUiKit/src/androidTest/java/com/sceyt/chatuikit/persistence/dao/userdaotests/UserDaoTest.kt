package com.sceyt.chatuikit.persistence.dao.userdaotests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.messages.SceytPresence
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UserDaoTest {

    private lateinit var database: SceytDatabase
    private lateinit var userDao: UserDao

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SceytDatabase::class.java,
        )
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // region Helpers

    private fun user(
        id: String,
        username: String = id,
        firstName: String? = "First$id",
        lastName: String? = "Last$id",
        status: String = "online",
        blocked: Boolean = false,
    ) = UserEntity(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        avatarURL = null,
        presence = SceytPresence(PresenceState.Online, status, 1L),
        activityStatus = UserState.Active,
        blocked = blocked,
    )

    private fun metadata(userId: String, key: String, value: String) = UserMetadataEntity(
        userId = userId,
        key = key,
        value = value,
    )

    private fun userDb(user: UserEntity, metadata: List<UserMetadataEntity> = emptyList()) = UserDb(
        user = user,
        metadata = metadata,
    )

    // endregion

    @Test
    fun insertUserWithMetadata_persistsUserAndMetadata() = runTest {
        userDao.insertUserWithMetadata(
            userDb(
                user("user1", firstName = "Alice"),
                listOf(metadata("user1", "team", "android")),
            )
        )

        val result = userDao.getUserById("user1")

        assertThat(result).isNotNull()
        assertThat(result!!.user.firstName).isEqualTo("Alice")
        assertThat(result.metadata.map { it.key to it.value }).containsExactly("team" to "android")
    }

    @Test
    fun insertUsersWithMetadata_replaceOnConflict_updatesExistingUserAndMetadata() = runTest {
        userDao.insertUsersWithMetadata(
            listOf(
                userDb(
                    user("user1", firstName = "Alice"),
                    listOf(metadata("user1", "role", "admin")),
                )
            )
        )

        userDao.insertUsersWithMetadata(
            listOf(
                userDb(
                    user("user1", firstName = "Alicia"),
                    listOf(metadata("user1", "role", "member")),
                )
            ),
            replaceUserOnConflict = true,
        )

        val result = userDao.getUserById("user1")
        assertThat(result!!.user.firstName).isEqualTo("Alicia")
        assertThat(result.metadata.map { it.key to it.value }).containsExactly("role" to "member")
    }

    @Test
    fun insertUsersWithMetadata_ignoreOnConflict_keepsExistingUserAndMetadata() = runTest {
        userDao.insertUsersWithMetadata(
            listOf(
                userDb(
                    user("user1", firstName = "Alice"),
                    listOf(metadata("user1", "role", "admin")),
                )
            )
        )

        userDao.insertUsersWithMetadata(
            listOf(
                userDb(
                    user("user1", firstName = "Alicia"),
                    listOf(metadata("user1", "role", "member")),
                )
            ),
            replaceUserOnConflict = false,
        )

        val result = userDao.getUserById("user1")
        assertThat(result!!.user.firstName).isEqualTo("Alice")
        assertThat(result.metadata.map { it.key to it.value }).containsExactly("role" to "admin")
    }

    @Test
    fun getUserByIdAsFlow_reflectsInsertedUser() = runTest {
        val flow = userDao.getUserByIdAsFlow("user1")
        assertThat(flow.first()).isNull()

        userDao.insertUserWithMetadata(userDb(user("user1", firstName = "Alice")))

        val result = flow.first()
        assertThat(result).isNotNull()
        assertThat(result!!.user.firstName).isEqualTo("Alice")
    }

    @Test
    fun getUsersById_returnsOnlyRequestedUsers() = runTest {
        userDao.insertUsersWithMetadata(
            listOf(
                userDb(user("user1")),
                userDb(user("user2")),
                userDb(user("user3")),
            )
        )

        val result = userDao.getUsersById(listOf("user1", "user3"))

        assertThat(result.map { it.id }).containsExactlyElementsIn(listOf("user1", "user3"))
    }

    @Test
    fun getUserIdsByDisplayName_matchesFirstNameLastNameAndFullNamePrefix() = runTest {
        userDao.insertUsersWithMetadata(
            listOf(
                userDb(user("user1", firstName = "Alice", lastName = "Smith")),
                userDb(user("user2", firstName = "Bob", lastName = "Stone")),
                userDb(user("user3", firstName = "Charlie", lastName = "Brown")),
            )
        )

        assertThat(userDao.getUserIdsByDisplayName("Ali")).containsExactly("user1")
        assertThat(userDao.getUserIdsByDisplayName("Stone")).containsExactly("user2")
        assertThat(userDao.getUserIdsByDisplayName("Alice Sm")).containsExactly("user1")
    }

    @Test
    fun searchUsersByMetadata_returnsOnlyUsersMatchingKeyAndValue() = runTest {
        userDao.insertUsersWithMetadata(
            listOf(
                userDb(user("user1"), listOf(metadata("user1", "team", "android"))),
                userDb(user("user2"), listOf(metadata("user2", "team", "ios"))),
                userDb(user("user3"), listOf(metadata("user3", "role", "admin"))),
            )
        )

        val teamResult = userDao.searchUsersByMetadata(listOf("team"), "roid")
        val roleResult = userDao.searchUsersByMetadata(listOf("role", "team"), "adm")

        assertThat(teamResult.map { it.id }).containsExactly("user1")
        assertThat(roleResult.map { it.id }).containsExactly("user3")
    }

    @Test
    fun updateUsers_updatesExistingRows() = runTest {
        userDao.insertUsersWithMetadata(listOf(userDb(user("user1", firstName = "Alice"))))

        userDao.updateUsers(listOf(user("user1", firstName = "Alicia", blocked = true)))

        val result = userDao.getUserById("user1")
        assertThat(result!!.user.firstName).isEqualTo("Alicia")
        assertThat(result.user.blocked).isTrue()
    }

    @Test
    fun updateUserStatus_updatesPresenceStatus() = runTest {
        userDao.insertUsersWithMetadata(listOf(userDb(user("user1", status = "available"))))

        userDao.updateUserStatus("user1", "busy")

        assertThat(userDao.getUserById("user1")!!.user.presence!!.status).isEqualTo("busy")
    }

    @Test
    fun blockUnBlockUser_updatesBlockedFlag() = runTest {
        userDao.insertUsersWithMetadata(listOf(userDb(user("user1", blocked = false))))

        userDao.blockUnBlockUser("user1", true)
        assertThat(userDao.getUserById("user1")!!.user.blocked).isTrue()

        userDao.blockUnBlockUser("user1", false)
        assertThat(userDao.getUserById("user1")!!.user.blocked).isFalse()
    }
}
