import io.libp2p.core.dsl.host
import io.libp2p.protocol.Ping

fun main() {
    val node = host {
        identity {
            random()
        }

        protocols {
            add(Ping())
        }

        network {
            listen("/ip4/0.0.0.0/tcp/0")
        }
    }

    node.start().get()

    println("id: ${node.peerId}")
    println("address: ${node.listenAddresses()}")
}