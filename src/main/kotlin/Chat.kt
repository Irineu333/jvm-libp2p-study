import io.libp2p.core.ConnectionClosedException
import io.libp2p.core.Stream
import io.libp2p.core.multistream.StrictProtocolBinding
import io.libp2p.protocol.ProtocolHandler
import io.libp2p.protocol.ProtocolMessageHandler
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

class Chat() : StrictProtocolBinding<ChatController>("/chat/1.0.0", ChatProtocol())

interface ChatController {
    fun send(message: String): CompletableFuture<Long>
}

class ChatProtocol : ProtocolHandler<ChatController>(Long.MAX_VALUE, Long.MAX_VALUE) {

    override fun onStartInitiator(stream: Stream): CompletableFuture<ChatController> {
        val handler = ChatInitiator()
        stream.pushHandler(handler)
        return handler.activeFuture
    }

    override fun onStartResponder(stream: Stream): CompletableFuture<ChatController> {
        val handler = ChatResponder()
        stream.pushHandler(handler)
        return CompletableFuture.completedFuture(handler)
    }

    class ChatResponder : ProtocolMessageHandler<ByteBuf>, ChatController {
        override fun onMessage(stream: Stream, msg: ByteBuf) {
            val messageBytes = ByteArray(msg.readableBytes())
            msg.readBytes(messageBytes)
            val message = String(messageBytes, StandardCharsets.UTF_8)
            println("Received: $message")
        }

        override fun send(message: String): CompletableFuture<Long> {
            throw RuntimeException("This is responder only")
        }
    }

    class ChatInitiator : ProtocolMessageHandler<ByteBuf>, ChatController {
        val activeFuture = CompletableFuture<ChatController>()
        lateinit var stream: Stream

        override fun onActivated(stream: Stream) {
            this.stream = stream
            activeFuture.complete(this)
        }

        override fun onMessage(stream: Stream, msg: ByteBuf) = Unit

        override fun onClosed(stream: Stream) {
            activeFuture.completeExceptionally(ConnectionClosedException())
        }

        override fun send(message: String): CompletableFuture<Long> {
            val messageBytes = message.toByteArray(StandardCharsets.UTF_8)
            val buffer = Unpooled.wrappedBuffer(messageBytes)

            return try {
                stream.writeAndFlush(buffer)
                CompletableFuture.completedFuture(messageBytes.size.toLong())
            } catch (e: Exception) {
                CompletableFuture.failedFuture(e)
            }
        }
    }
}