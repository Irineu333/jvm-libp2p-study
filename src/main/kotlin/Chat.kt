import io.libp2p.core.PeerId
import io.libp2p.core.Stream
import io.libp2p.core.multistream.StrictProtocolBinding
import io.libp2p.etc.types.toByteBuf
import io.libp2p.protocol.ProtocolHandler
import io.libp2p.protocol.ProtocolMessageHandler
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

typealias OnChatMessage = (PeerId, String) -> Unit

class Chat(onMessage: OnChatMessage) : ChatBinding(ChatProtocol(onMessage))

interface ChatController {
    fun send(message: String)
}

open class ChatBinding(echo: ChatProtocol) : StrictProtocolBinding<ChatController>("chat/0.1.0", echo)

open class ChatProtocol(
    private val onMessage: OnChatMessage
) : ProtocolHandler<ChatController>(Long.MAX_VALUE, Long.MAX_VALUE) {

    override fun onStartInitiator(stream: Stream) = onStart(stream)
    override fun onStartResponder(stream: Stream) = onStart(stream)

    private fun onStart(stream: Stream): CompletableFuture<ChatController> {
        val ready = CompletableFuture<Void>()
        val handler = Chatter(onMessage, ready)
        stream.pushHandler(handler)
        return ready.thenApply { handler }
    }

    open inner class Chatter(
        private val onMessage: OnChatMessage,
        val ready: CompletableFuture<Void>
    ) : ProtocolMessageHandler<ByteBuf>, ChatController {
        lateinit var stream: Stream

        override fun onActivated(stream: Stream) {
            this.stream = stream
            ready.complete(null)
        }

        override fun onMessage(stream: Stream, msg: ByteBuf) {
            val msgStr = msg.toString(Charset.defaultCharset())
            onMessage(stream.remotePeerId(), msgStr)
        }

        override fun send(message: String) {
            val data = message.toByteArray(Charset.defaultCharset())
            stream.writeAndFlush(data.toByteBuf())
        }
    }
}