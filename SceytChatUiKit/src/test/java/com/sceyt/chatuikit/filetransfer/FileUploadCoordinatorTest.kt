package com.sceyt.chatuikit.filetransfer

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.logicimpl.RecordingFileTransferTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FileUploadCoordinatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `normal preparation is ordered while shared upload runs concurrently`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileUploadCoordinator(this) { transport }
        val prepared = mutableListOf<String>()
        fun submit(id: String, sharingKey: String? = null) {
            coordinator.upload(id, id, sharingKey, sequential = sharingKey == null,
                prepare = {
                    prepared += id
                    FileUploadPreparation.Ready(request(id))
                }, observer = Results(), bind = {},
            )
        }
        submit("first")
        submit("second")
        submit("shared", "source")
        assertThat(prepared).containsExactly("first", "shared").inOrder()

        transport.uploadCalls.first().succeed("first-url")
        assertThat(prepared).containsExactly("first", "shared", "second").inOrder()
        transport.uploadCalls[1].succeed("shared-url")
        transport.uploadCalls[2].succeed("second-url")
    }

    @Test
    fun `shared pause resume follows participants rather than deleted owner`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport().apply { pauseResult = true; resumeResult = true }
        val coordinator = FileUploadCoordinator(this) { transport }
        val owner = subscribe(coordinator, "owner", sharingKey = "source")
        val results = Results()
        val follower = subscribe(coordinator, "follower", results, sharingKey = "source")
        owner.pause()
        assertThat(transport.pauseCalls).isEmpty()
        follower.pause()
        assertThat(transport.pauseCalls).containsExactly("owner")
        follower.resume()
        owner.cancel()
        follower.pause()
        follower.resume()

        assertThat(transport.resumeCalls).containsExactly("owner", "owner").inOrder()
        assertThat(transport.uploadCalls).hasSize(1)
        transport.uploadCalls.single().succeed("shared-url")
        assertThat(results.values.single().getOrThrow()).isEqualTo("shared-url")
    }

    @Test
    fun `paused successful member retains its result without trapping a new share`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileUploadCoordinator(this) { transport }
        subscribe(coordinator, "owner", sharingKey = "source")
        val results = Results()
        val paused = subscribe(coordinator, "paused", results, sharingKey = "source")
        paused.pause()
        transport.uploadCalls.first().succeed("first-url")
        subscribe(coordinator, "new-share", sharingKey = "source")

        paused.resume()
        assertThat(results.values.single().getOrThrow()).isEqualTo("first-url")
        assertThat(transport.uploadCalls).hasSize(2)
        transport.uploadCalls.last().succeed("new-url")
    }

    @Test
    fun `failed group allows a new share and its paused member reuses the new result`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileUploadCoordinator(this) { transport }
        subscribe(coordinator, "owner", sharingKey = "source")
        val results = Results()
        val paused = subscribe(coordinator, "paused", results, sharingKey = "source")
        paused.pause()
        transport.uploadCalls.first().fail(IOException("failed"))
        subscribe(coordinator, "new-share", sharingKey = "source")
        transport.uploadCalls.last().succeed("new-url")

        paused.resume()
        assertThat(results.values.single().getOrThrow()).isEqualTo("new-url")
        assertThat(transport.uploadCalls).hasSize(2)
    }

    @Test
    fun `pause from start prevents preparation and optimistic transport pause`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport().apply { pauseResult = true }
        val coordinator = FileUploadCoordinator(this) { transport }
        lateinit var subscription: FileTransferSubscription
        var prepared = false
        coordinator.upload("file", "file", prepare = {
            prepared = true
            FileUploadPreparation.Ready(request("file"))
        }, observer = object : FileTransferObserver {
            override fun onStarted() = subscription.pause()
            override fun onResult(result: Result<String>) = error("Paused upload must not complete")
        }, bind = { subscription = it })

        assertThat(prepared).isFalse()
        assertThat(transport.uploadCalls).isEmpty()
        assertThat(transport.pauseCalls).isEmpty()
        coordinator.cancelAll()
    }

    @Test
    fun `optional concurrent upload cannot fail or delay the main upload`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileUploadCoordinator(this) { transport }
        val main = Results()
        subscribe(coordinator, "main", main)
        subscribe(coordinator, "next")
        subscribe(coordinator, "optional", sequential = false)
        assertThat(transport.uploadCalls.map { it.request.operationId }).containsExactly("main", "optional").inOrder()

        transport.uploadCalls.last().fail(IOException("optional failure"))
        assertThat(main.values).isEmpty()
        transport.uploadCalls.first().succeed("main-url")
        assertThat(main.values.single().getOrThrow()).isEqualTo("main-url")
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("next")
        transport.uploadCalls.last().succeed("next-url")
    }

    @Test
    fun `natively paused upload finishing while queued does not stall the queue`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport().apply { pauseResult = true; resumeResult = true }
        val coordinator = FileUploadCoordinator(this) { transport }
        val paused = subscribe(coordinator, "paused")
        paused.pause()
        subscribe(coordinator, "current")
        paused.resume()
        transport.uploadCalls[0].fail(IOException("failed while paused"))
        transport.uploadCalls[1].succeed("current-url")

        val results = Results()
        subscribe(coordinator, "next", results)
        assertThat(transport.uploadCalls).hasSize(3)
        transport.uploadCalls.last().succeed("next-url")
        assertThat(results.values.single().getOrThrow()).isEqualTo("next-url")
    }

    @Test
    fun `pause arriving after transport result keeps the result`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileUploadCoordinator(this) { transport }
        lateinit var subscription: FileTransferSubscription
        val values = mutableListOf<Result<String>>()
        val observer = object : FileTransferObserver {
            override fun onProgress(progressPercent: Float) {
                subscription.pause()
                transport.uploadCalls.single().succeed("url")
            }

            override fun onResult(result: Result<String>) { values += result }
        }
        coordinator.upload("file", "file", null, true,
            prepare = { FileUploadPreparation.Ready(request("file")) },
            observer = observer, bind = { subscription = it },
        )

        transport.uploadCalls.single().progress(50f)

        assertThat(values.single().getOrThrow()).isEqualTo("url")
        assertThat(transport.uploadCalls).hasSize(1)
    }

    private fun subscribe(
        coordinator: FileUploadCoordinator,
        id: String,
        observer: Results = Results(),
        sharingKey: String? = null,
        sequential: Boolean = sharingKey == null,
    ): FileTransferSubscription {
        lateinit var subscription: FileTransferSubscription
        coordinator.upload(id, id, sharingKey, sequential,
            prepare = { FileUploadPreparation.Ready(request(id)) },
            observer = observer, bind = { subscription = it },
        )
        return subscription
    }

    private fun request(id: String) = FileUploadRequest(id, temporaryFolder.newFile(), "file.bin", null)

    private class Results : FileTransferObserver {
        val values = mutableListOf<Result<String>>()
        override fun onResult(result: Result<String>) { values += result }
    }
}
