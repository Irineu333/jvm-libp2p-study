import io.libp2p.core.PeerId
import io.libp2p.core.dsl.host
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.protocol.Ping

fun main() {

    val node = host {
        identity { random() }
        protocols {
            +Ping()
        }
        network {
            listen("/ip4/127.0.0.1/tcp/0")
        }
    }

    node.start().get()

    println()
    println("Node started")
    println("id: ${node.peerId}")
    println("address: ${node.listenAddresses().single()}")
    println()

    do {

        println("1. Connect to peer")
        println("2. List peers")
        println("3. Ping")
        println("0. Exit")
        print("> ")

        val op = readln().toInt()

        println()

        when (op) {
            1 -> {
                print("Peer address: ")
                val address = Multiaddr(readln())
                val connection = node.network.connect(address).get()
                println("Connected to ${connection.secureSession().remoteId}")
            }

            2 -> {
                val connections = node.network.connections.takeIf { it.isNotEmpty() }

                connections?.let {
                    println("Peers:")
                    connections.forEach {
                        println(it.secureSession().remoteId)
                    }
                } ?: println("No peers connected")
            }

            3 -> {
                print("Peer: ")
                val peerId = readln()

                Ping().dial(node, PeerId.fromBase58(peerId))
                    .controller
                    .thenCompose { controller ->
                        controller.ping()
                    }.thenAccept { latency ->
                        println("Ping: $latency ms")
                    }.join()
            }
        }

        println()

    } while (op != 0)

    node.stop().get()
    println("Node stopped")
}