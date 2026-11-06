package com.sceyt.chatuikit.persistence.file_transfer

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** Serves the ciphertext fixture with the same byte ranges used by a CDN. */
internal class AttachmentHttpServer(private val file: File) : AutoCloseable {
    private val server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
    private val workers = Executors.newCachedThreadPool()
    private val clients = ConcurrentHashMap.newKeySet<Socket>()
    private val acceptor = Thread {
        while (!server.isClosed) {
            try {
                val client = server.accept()
                clients.add(client)
                workers.execute { respond(client) }
            } catch (_: IOException) {
                break
            }
        }
    }.apply { start() }

    val url: String = "http://127.0.0.1:${server.localPort}/attachment"
    val requests = CopyOnWriteArrayList<Map<String, String>>()

    @Volatile
    var chunkDelayMillis = 0L

    @Volatile
    var ignoreRanges = false

    @Volatile
    var redirectTo: String? = null

    private fun respond(client: Socket) {
        try {
            client.use {
                val input = client.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                val request = input.readLine() ?: return
                val headers = generateSequence { input.readLine() }
                    .takeWhile { it.isNotEmpty() }.toList()
                requests += headers.associate { header ->
                    val (name, value) = header.split(':', limit = 2)
                    name.lowercase() to value.trim()
                }
                val output = client.getOutputStream()
                val redirect = redirectTo
                if (redirect != null) {
                    val response = "HTTP/1.1 302 Found\r\nLocation: $redirect\r\n" +
                            "Content-Length: 0\r\nConnection: close\r\n\r\n"
                    output.write(response.toByteArray(Charsets.ISO_8859_1))
                    return
                }
                val range = headers.firstOrNull { header ->
                    !ignoreRanges && header.startsWith("Range:", ignoreCase = true)
                }?.substringAfter("bytes=")?.split('-', limit = 2)
                val start = range?.first()?.toLong() ?: 0L
                val lastByte = file.length() - 1
                val end = minOf(range?.get(1)?.toLongOrNull() ?: lastByte, lastByte)
                val length = end - start + 1
                val status = if (range == null) "200 OK" else "206 Partial Content"
                val response = buildString {
                    append("HTTP/1.1 $status\r\n")
                    append("Content-Length: $length\r\n")
                    append("ETag: \"attachment-fixture\"\r\n")
                    if (range != null) append("Content-Range: bytes $start-$end/${file.length()}\r\n")
                    append("Connection: close\r\n\r\n")
                }
                output.write(response.toByteArray(Charsets.ISO_8859_1))
                if (request.startsWith("HEAD ")) return

                RandomAccessFile(file, "r").use { source ->
                    source.seek(start)
                    val buffer = ByteArray(8192)
                    var remaining = length
                    while (remaining > 0) {
                        val readSize = minOf(remaining, buffer.size.toLong()).toInt()
                        val count = source.read(buffer, 0, readSize)
                        check(count > 0) { "Ciphertext fixture ended early" }
                        output.write(buffer, 0, count)
                        remaining -= count
                        if (chunkDelayMillis > 0) Thread.sleep(chunkDelayMillis)
                    }
                }
            }
        } catch (_: IOException) {
            // Canceling a native request closes the client's connection.
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            clients.remove(client)
        }
    }

    override fun close() {
        server.close()
        acceptor.join()
        clients.forEach { runCatching { it.close() } }
        workers.shutdownNow()
    }
}
