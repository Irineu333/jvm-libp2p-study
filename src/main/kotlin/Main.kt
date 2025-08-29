import io.libp2p.core.dsl.host
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.protocol.Ping

fun main() {
    val node1 = host {
        identity { random() }
        protocols { add(Ping()) }
        network {
            listen("/ip4/127.0.0.1/tcp/4001")
        }
    }

    val node2 = host {
        identity { random() }
        protocols { add(Ping()) }
        network {
            listen("/ip4/127.0.0.1/tcp/4002")
        }
    }

    node1.start().get()
    node2.start().get()

    val addr = Multiaddr("/ip4/127.0.0.1/tcp/4001/p2p/${node1.peerId}")
    node2.network.connect(addr).get()

    println("Connected")
}