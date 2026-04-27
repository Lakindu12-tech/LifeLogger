package com.example.lifelogger.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.lifelogger.data.database.dao.LogEntryDao
import com.example.lifelogger.data.model.LogEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class LogEntryRepositoryFlowTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()


    private val fakeDao = FakeLogEntryDao()
    private val repository = LogEntryRepository(fakeDao)

    @Test
    fun saveEntry_thenItAppearsInPreviousEntries_andCanBeOpenedWithAudioInfo() = runBlocking {
        val createdId = repository.insertEntry(
            LogEntry(
                userId = "test-user",
                title = "Test title",
                content = "This content simulates adding a new entry",
                category = "general",
                audioUri = "C:/tmp/fake_audio.m4a",
                timestamp = 1_700_000_000_000
            )
        )

        val entries = repository.getAllEntries().getOrAwaitValue()
        assertTrue(entries.any { it.id == createdId })

        val openedEntry = repository.getEntryById(createdId)
        assertNotNull(openedEntry)
        assertEquals("This content simulates adding a new entry", openedEntry?.content)
        assertEquals("C:/tmp/fake_audio.m4a", openedEntry?.audioUri)
        assertFalse(openedEntry?.audioUri.isNullOrBlank())
    }

    @Test
    fun newestEntry_isReturnedFirstInPreviousEntriesList() = runBlocking {
        repository.insertEntry(
            LogEntry(
                userId = "test-user",
                title = "Older",
                content = "older content",
                timestamp = 1000L
            )
        )
        repository.insertEntry(
            LogEntry(
                userId = "test-user",
                title = "Newer",
                content = "newer content",
                timestamp = 2000L
            )
        )

        val entries = repository.getAllEntries().getOrAwaitValue()

        assertEquals("Newer", entries.first().title)
        assertEquals("Older", entries.last().title)
    }

    @Test
    fun searchEntries_returnsOnlyMatchingSavedContent() = runBlocking {
        repository.insertEntry(
            LogEntry(
                userId = "test-user",
                title = "Workout log",
                content = "Did interval training",
                timestamp = 3000L
            )
        )
        repository.insertEntry(
            LogEntry(
                userId = "test-user",
                title = "Study notes",
                content = "Read chapter five",
                timestamp = 4000L
            )
        )

        val results = repository.searchEntries("interval").getOrAwaitValue()

        assertEquals(1, results.size)
        assertEquals("Workout log", results.single().title)
    }
}

private class FakeLogEntryDao : LogEntryDao {
    private val entries = mutableListOf<LogEntry>()
    private var nextId = 1L

    override suspend fun insert(entry: LogEntry): Long {
        val id = if (entry.id == 0L) nextId++ else entry.id
        entries.removeAll { it.id == id }
        entries.add(entry.copy(id = id))
        return id
    }

    override suspend fun update(entry: LogEntry) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            entries[index] = entry
        }
    }

    override suspend fun delete(entry: LogEntry) {
        entries.removeAll { it.id == entry.id }
    }

    override fun getAllEntries(): LiveData<List<LogEntry>> {
        return MutableLiveData(entries.sortedByDescending { it.timestamp })
    }

    override fun getEntriesByUser(userId: String): LiveData<List<LogEntry>> {
        return MutableLiveData(
            entries.filter { it.userId == userId }.sortedByDescending { it.timestamp }
        )
    }

    override suspend fun getEntryById(id: Long): LogEntry? {
        return entries.firstOrNull { it.id == id }
    }

    override suspend fun getEntryByIdForUser(id: Long, userId: String): LogEntry? {
        return entries.firstOrNull { it.id == id && it.userId == userId }
    }

    override suspend fun getUnsyncedEntries(): List<LogEntry> {
        return entries.filter { !it.isSynced }
    }

    override suspend fun getUnsyncedEntriesByUser(userId: String): List<LogEntry> {
        return entries.filter { !it.isSynced && it.userId == userId }
    }

    override suspend fun markAsSynced(id: Long) {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            entries[index] = entries[index].copy(isSynced = true)
        }
    }

    override fun searchEntries(searchQuery: String): LiveData<List<LogEntry>> {
        val term = searchQuery.trim('%').lowercase()
        return MutableLiveData(
            entries.filter {
                it.content.lowercase().contains(term) || it.title.lowercase().contains(term)
            }.sortedByDescending { it.timestamp }
        )
    }

    override fun searchEntriesByUser(userId: String, searchQuery: String): LiveData<List<LogEntry>> {
        val term = searchQuery.trim('%').lowercase()
        return MutableLiveData(
            entries.filter {
                it.userId == userId &&
                    (it.content.lowercase().contains(term) || it.title.lowercase().contains(term))
            }.sortedByDescending { it.timestamp }
        )
    }
}

private fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }

    observeForever(observer)

    if (!latch.await(time, timeUnit)) {
        removeObserver(observer)
        throw TimeoutException("LiveData value was never set.")
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}

